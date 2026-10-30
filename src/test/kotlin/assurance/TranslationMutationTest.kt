package dev.mtg.assistant

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.tooling.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

private fun altered(card: CardDefinition, transform: (JsonObject)->JsonElement): CardDefinition {
    fun walk(e: JsonElement): JsonElement = when(e) {
        is JsonArray -> JsonArray(e.map(::walk))
        is JsonObject -> transform(JsonObject(e.mapValues { walk(it.value) }))
        else -> e
    }
    return CardLoader.fromJsonPreservingIds(walk(Json.parseToJsonElement(CardExporter.exportToJson(card))).toString())
}
private fun JsonObject.kind()=get("type")?.jsonPrimitive?.content
private fun JsonObject.with(key: String,value: JsonElement)=JsonObject(this+mapOf(key to value))

/** Small source-derived review probes, not a universal semantics checker or Oracle parser. */
internal fun requirementProbe(c: CardDefinition): List<String> {
    val root=Json.parseToJsonElement(CardExporter.exportToJson(c))
    fun nodes(e: JsonElement): List<JsonObject> = when(e) {
        is JsonArray -> e.flatMap(::nodes)
        is JsonObject -> listOf(e)+e.values.flatMap(::nodes)
        else -> emptyList()
    }
    val ns=nodes(root);val errors=mutableListOf<String>()
    fun expect(id: String, value: Boolean) {if(!value) errors+=id}
    when(c.oracleId) {
        PredatoryUrge.oracleId -> {
            expect("PU1",c.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.cost==Costs.Tap)
            val damage=ns.filter {it.kind()=="DealDamage"}
            expect("PU4",damage.first()["damageSource"]!=JsonPrimitive("Self"))
        }
        LeyWeaver.oracleId -> {
            expect("LW1-optional",ns.any {it["gate"]==JsonPrimitive("Gate.MayDecide")})
            expect("LW1-controller",ns.any {it["players"]==JsonPrimitive("TargetPlayer")})
            expect("LW1-reveal",ns.any {it.kind()=="MoveCollection" && it["revealed"]==JsonPrimitive(true)})
        }
        HourOfPromise.oracleId -> {
            val effects=root.jsonObject.getValue("script").jsonObject.getValue("spellEffect").jsonObject.getValue("effects").jsonArray
            expect("HP3",effects.indexOfFirst {it is JsonObject && it.kind()=="Gated"}>effects.indexOfFirst {it is JsonObject && it.kind()=="MoveCollection"})
            expect("HP1",ns.any {it.kind()=="ToZone" && it["zone"]==JsonPrimitive("Battlefield") && it["placement"]==JsonPrimitive("Tapped")})
        }
        Tatterkite.oracleId -> expect("TK2",c.staticAbilities.any {it is CantReceiveCounters})
        SwordOfTruthAndJustice.oracleId -> expect("SJ3",c.triggeredAbilities.single().targetRequirement==null)
    }
    return errors
}

class TranslationMutationTest : FunSpec({
    data class Spec(val id: String,val defect: String,val original: CardDefinition,val changed: CardDefinition,val check: (CardDefinition)->Unit)
    val grant=PredatoryUrge.staticAbilities.filterIsInstance<GrantActivatedAbility>().single()
    val swordTrigger=SwordOfTruthAndJustice.triggeredAbilities.single()
    val specs=listOf(
        Spec("M1","live reference loses last-known damage",PredatoryUrge,altered(PredatoryUrge) {
            if(it.kind()=="DealDamage" && it["target"]!=JsonPrimitive("Self")) it.with("damageSource",JsonPrimitive("Self")) else it
        },{ReliabilityChecks.urge(it,removeHost=true)}),
        Spec("M2","tap cost omitted",PredatoryUrge,PredatoryUrge.copy(script=PredatoryUrge.script.copy(staticAbilities=listOf(grant.copy(ability=grant.ability.copy(cost=Costs.Free))))),{ReliabilityChecks.urge(it)}),
        Spec("M3","optional search made mandatory",LeyWeaver,altered(LeyWeaver) {if(it.kind()=="Gated") it.getValue("then") else it},{ReliabilityChecks.ley(it)}),
        Spec("M4","targeted player's choice assigned to controller",LeyWeaver,altered(LeyWeaver) {if(it.kind()=="IterationSpace.Players") it.with("players",JsonPrimitive("You")) else it},{ReliabilityChecks.ley(it,accept=true)}),
        Spec("M5","condition moved before fetched lands",HourOfPromise,altered(HourOfPromise) {
            if(it.kind()=="Composite" && it["effects"]!!.jsonArray.any {e -> e is JsonObject && e.kind()=="Gated"}) {
                val es=it.getValue("effects").jsonArray;it.with("effects",JsonArray(listOf(es.last())+es.dropLast(1)))
            } else it
        },{ReliabilityChecks.hour(it)}),
        Spec("M6","fetched cards go to hand instead of battlefield",HourOfPromise,altered(HourOfPromise) {
            if(it.kind()=="ToZone" && it["zone"]==JsonPrimitive("Battlefield")) it.with("zone",JsonPrimitive("Hand")) else it
        },{ReliabilityChecks.hour(it)}),
        Spec("M7","counter prohibition omitted",Tatterkite,Tatterkite.copy(script=Tatterkite.script.copy(staticAbilities=emptyList())),{ReliabilityChecks.kite(it)}),
        Spec("M8","nontargeting choice replaced by a target",SwordOfTruthAndJustice,SwordOfTruthAndJustice.copy(script=SwordOfTruthAndJustice.script.copy(triggeredAbilities=listOf(swordTrigger.copy(
            targetRequirement=TargetObject(filter=TargetFilter.CreatureYouControl),
            effect=Effects.AddCounters(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE,1,EffectTarget.ContextTarget(0)) then Effects.Proliferate()
        )))),{ReliabilityChecks.sword(it)}),
        Spec("M9","required reveal omitted",LeyWeaver,altered(LeyWeaver) {if(it.kind()=="MoveCollection") it.with("revealed",JsonPrimitive(false)) else it},{ReliabilityChecks.ley(it,accept=true)})
    )
    val observations=mutableListOf<JsonObject>()
    for(s in specs) test("${s.id} structurally valid semantic defect detected with unchanged control") {
        (definitionDigest(s.changed)==definitionDigest(s.original)) shouldBe false
        val originalLint=CardValidator.validate(s.original);val lint=CardValidator.validate(s.changed)
        lint.count {it.severity==LintSeverity.ERROR} shouldBe 0
        requirementProbe(s.original) shouldBe emptyList()
        var controlFailure: AssertionError?=null
        try {s.check(s.original)} catch(e: AssertionError) {controlFailure=e}
        // Every mutation must be discriminated from a passing source-correct control.
        controlFailure shouldBe null
        var failure: AssertionError?=null
        try {s.check(s.changed)} catch(e: AssertionError) {failure=e}
        val violations=requirementProbe(s.changed)
        observations+=buildJsonObject {
            put("id",s.id);put("defect",s.defect);put("oracleId",s.original.oracleId);put("productionRegistered",false)
            put("validatorErrors",lint.count {it.severity==LintSeverity.ERROR})
            put("validatorWarnings",JsonArray(lint.filter {it.severity==LintSeverity.WARNING}.map {JsonPrimitive(it.message)}))
            put("controlValidatorWarnings",JsonArray(originalLint.filter {it.severity==LintSeverity.WARNING}.map {JsonPrimitive(it.message)}))
            put("sourceProbeViolations",JsonArray(violations.map(::JsonPrimitive)))
            put("executionDetected",failure!=null && controlFailure==null);
            put("executionConfounded",controlFailure!=null);put("controlAssertion",controlFailure?.message ?: "");put("assertion",failure?.message ?: "MISSED")
            put("controlPassed",controlFailure==null)
            put("qualificationWithoutIndependentApproval",assessTranslation(s.changed,null).qualification.name)
        }
        (failure!=null) shouldBe true
        violations.isNotEmpty() shouldBe true
    }
    afterSpec {
        val root = TestFiles.root
        val path=root.resolve("generated/translation-reliability/mutations.json");Files.createDirectories(path.parent)
        Files.writeString(path,Json {prettyPrint=true}.encodeToString(JsonArray.serializer(),JsonArray(observations)))
    }
})
