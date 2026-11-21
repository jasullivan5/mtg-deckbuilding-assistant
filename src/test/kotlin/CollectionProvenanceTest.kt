package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class CollectionProvenanceTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extract(effect: Effect) = PathwayExtractor().extract(listOf(checkNotNull(catalog.card("Cloudshift")).let {
        it.copy(script=it.script.copy(spellEffect=effect))
    }))
    val gather=GatherCardsEffect(CardSource.TopOfLibrary(1),"input")
    val move=MoveCollectionEffect("input",CardDestination.ToZone(Zone.EXILE),storeMovedAs="moved")
    fun use(variable: String="moved", index: Int=0)=Effects.Move(EffectTarget.PipelineTarget(variable,index),Zone.HAND)
    test("frozen four-card source cohort is bound to the actual native definitions") {
        val root=Path.of(System.getProperty("assistant.projectRoot"))
        val engine=System.getenv("ARGENTUM_ENGINE_DIR")?.let(Path::of) ?: root.parent.resolve("argentum-engine")
        val sources=Json.parseToJsonElement(Files.readString(root.resolve("src/test/resources/regression/collection-movement-sources.json"))).jsonObject
        sources.getValue("engineRevision").jsonPrimitive.content.length shouldBe 40
        for(c in sources.getValue("cases").jsonArray.map { it.jsonObject }) {
            val source=Files.readString(engine.resolve(c.getValue("nativePath").jsonPrimitive.content)).replace("\r\n","\n").toByteArray()
            MessageDigest.getInstance("SHA-256").digest(source).joinToString("") { "%02x".format(it) } shouldBe c.getValue("nativeLfSha256").jsonPrimitive.content
        }
    }
    test("Savior has one optional union requirement with clause-specific restrictions and movement facts") {
        val x=PathwayExtractor().extract(listOf(checkNotNull(catalog.card("Savior of Ollenbock"))))
        val union=x.actions.flatMap { it.unionTargets }.single()
        union.minimum shouldBe 0
        union.maximum shouldBe 1
        union.clauses.map { it.zone } shouldBe listOf(Zone.BATTLEFIELD,Zone.GRAVEYARD)
        union.clauses.map { it.excludeSelf } shouldBe listOf(true,false)
        union.support shouldBe Verdict.MATCH
        union.operationPaths.size shouldBe 1
        union.movementFactIds.size shouldBe 2
        union.movementFactIds.all { id -> x.facts.any { it.id==id } } shouldBe true
    }
    test("a moved-result is a distinct bounded binding and consumers reference its producing fact") {
        val x=extract(CompositeEffect(listOf(gather,move,use())))
        val action=x.actions.single { it.collectionStages.isNotEmpty() }
        val result=action.collectionStages.single { it.membership==CollectionMembership.NATIVE_MOVED_RESULT }
        result.maximum shouldBe 1
        result.minimum shouldBe 0
        result.sourceZone shouldBe Zone.LIBRARY
        result.destinationZone shouldBe Zone.EXILE
        result.movementFactIds.size shouldBe 1
        result.id shouldBe action.collectionUses.last().producerId
        action.collectionUses.last().support shouldBe Verdict.MATCH
        result.inputId shouldBe action.collectionStages.first().id
        action.collectionStages.single { it.membership==CollectionMembership.RETAINED_INPUT }.id.let { it!=result.id } shouldBe true
    }
    test("real bounded pipelines support distinct indexed consumers without certifying delayed obligations") {
        for(name in listOf("Cryptic Coat","Come Back Wrong","Kheru Lich Lord")) {
            val x=PathwayExtractor().extract(listOf(checkNotNull(catalog.card(name))))
            val stages=x.actions.flatMap { it.collectionStages }
            stages.any { it.membership==CollectionMembership.NATIVE_MOVED_RESULT && it.maximum==1 } shouldBe true
            x.actions.flatMap { it.collectionUses }.filter { it.consumer=="INDEXED_OBJECT" }.isNotEmpty() shouldBe true
        }
    }
    test("empty and out-of-bounds indexed members cannot establish an operation") {
        for(index in listOf(-1,1,2)) {
            extract(CompositeEffect(listOf(gather,move,use(index=index)))).actions.flatMap { it.collectionUses }.last().support shouldBe Verdict.NO_MATCH
        }
        extract(CompositeEffect(listOf(gather.copy(source=CardSource.TopOfLibrary(0)),move,use()))).actions.flatMap { it.collectionUses }.last().support shouldBe Verdict.NO_MATCH
    }
    test("branch-local bindings never escape and unrelated variable names never join") {
        val effect=CompositeEffect(listOf(ConditionalOnCollectionEffect("absent",CompositeEffect(listOf(gather,move))),use()))
        extract(effect).actions.flatMap { it.collectionUses }.last().producerId shouldBe null
        extract(CompositeEffect(listOf(gather,move,use("different")))).actions.flatMap { it.collectionUses }.last().support shouldBe Verdict.UNKNOWN
    }
    test("reading original input after a movement does not claim successful result membership") {
        val x=extract(CompositeEffect(listOf(gather,move,use("input"))))
        x.actions.flatMap { it.collectionUses }.last().support shouldBe Verdict.UNKNOWN
    }
    test("the same variable in separate actions and physical cards cannot share a producer") {
        val base=checkNotNull(catalog.card("Cloudshift"))
        val producer=base.copy(script=base.script.copy(spellEffect=CompositeEffect(listOf(gather,move))))
        val other=base.copy(name="Separate original",script=base.script.copy(spellEffect=use()))
        val actions=PathwayExtractor().extract(listOf(producer,other)).actions
        actions.single { it.card==other.name && it.collectionUses.isNotEmpty() }.collectionUses.single().producerId shouldBe null
    }
    test("a union with correlated multiple targets or an unsupported chooser remains unresolved") {
        val original=checkNotNull(catalog.card("Savior of Ollenbock"))
        val union=PathwayExtractor().extract(listOf(original)).actions.flatMap { it.unionTargets }.single().requirement
        for(target in listOf(union.copy(count=2),union.copy(chooser=TargetChooser.Opponent))) {
            val base=checkNotNull(catalog.card("Cloudshift"))
            val x=PathwayExtractor().extract(listOf(base.copy(script=base.script.copy(targetRequirements=listOf(target)))))
            x.actions.flatMap { it.unionTargets }.single().support shouldBe Verdict.UNKNOWN
        }
    }
    test("selection remainder and filtered complement do not become complete moved subsets") {
        val select=SelectFromCollectionEffect("input",SelectionMode.ChooseUpTo(com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(1)),storeSelected="chosen",storeRemainder="remainder")
        extract(CompositeEffect(listOf(gather,select,use("remainder")))).actions.flatMap { it.collectionUses }.last().support shouldBe Verdict.UNKNOWN
        val filter=FilterCollectionEffect("input",storeMatching="yes",storeNonMatching="no")
        extract(CompositeEffect(listOf(gather,filter,use("no")))).actions.flatMap { it.collectionUses }.last().support shouldBe Verdict.UNKNOWN
    }
    test("the empty collection branch refines indexed membership to no object") {
        val x=extract(CompositeEffect(listOf(gather,ConditionalOnCollectionEffect("input",Effects.GainLife(1),use("input")))))
        x.actions.flatMap { it.collectionUses }.single { it.consumer=="INDEXED_OBJECT" }.support shouldBe Verdict.NO_MATCH
    }
    test("report decoding defaults new evidence to empty and preserves historical action fields") {
        val old=ActionEvidence("a","c","f","p",emptyList(),JsonObject(emptyMap()))
        val encoded=reportJson.encodeToString(ActionEvidence.serializer(),old)
        encoded.contains("collectionStages") shouldBe false
        reportJson.decodeFromString(ActionEvidence.serializer(),encoded) shouldBe old
    }
})
