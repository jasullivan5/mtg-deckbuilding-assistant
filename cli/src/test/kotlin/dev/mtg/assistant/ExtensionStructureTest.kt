package dev.mtg.assistant

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.tooling.CardExporter
import com.wingedsheep.sdk.tooling.CardLoader
import dev.mtg.assistant.extensions.Extensions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

/** External counterpart to upstream CardDefinitionSnapshotTest; never snapshots the upstream corpus. */
class ExtensionStructureTest : FunSpec({
    val root=if(Files.isDirectory(Path.of("cli"))) Path.of(".") else Path.of("..")
    test("registered extension trees match explicitly reviewed goldens and round-trip preserving ids") {
        val dir=root.resolve("cli/src/test/resources/extensions")
        if(System.getenv("UPDATE_EXTENSION_SNAPSHOTS")=="1") Files.createDirectories(dir)
        val expected=Extensions.all.map { it.definition.name.replace(Regex("[^A-Za-z0-9]+"),"_")+".json" }.sorted()
        Extensions.all.sortedBy { it.definition.name }.forEach { extension ->
            val c=extension.definition
            val json=CardExporter.exportToJson(c)
            val golden=dir.resolve(c.name.replace(Regex("[^A-Za-z0-9]+"),"_")+".json")
            if(System.getenv("UPDATE_EXTENSION_SNAPSHOTS")=="1") Files.writeString(golden,json+"\n")
            Files.readString(golden).trimEnd() shouldBe json.trimEnd()
            CardExporter.exportToJson(CardLoader.fromJsonPreservingIds(json)) shouldBe json
        }
        Files.list(dir).use { paths -> paths.map { it.fileName.toString() }.sorted().toList() } shouldBe expected
    }
    test("nested transform and embedded spell structures survive the SDK round-trip") {
        val embedded=Catalog(emptyList()).card("Emeritus of Conflict")!!
        val nested=card("Roundtrip Front") { typeLine="Creature — Human";power=1;toughness=1 }
            .copy(backFace=embedded)
        val json=CardExporter.exportToJson(nested)
        val loaded=CardLoader.fromJsonPreservingIds(json)
        loaded.backFace!!.cardFaces shouldBe embedded.cardFaces
        CardExporter.exportToJson(loaded) shouldBe json
    }
    test("external definitions use facade and typed pipeline boundaries") {
        val sources=root.resolve("cli/src/main/kotlin/dev/mtg/assistant/extensions")
        val violations=Files.list(sources).use { paths -> paths.filter { it.toString().endsWith(".kt") }.toList() }
            .flatMap { path -> authoringViolations(Files.readString(path)).map { "${path.fileName}: $it" } }
        violations shouldBe emptyList()
    }
    test("authoring boundary catches raw effects costs triggers amounts and string slots") {
        listOf("effect = DrawCardsEffect(1)","effect = com.wingedsheep.sdk.scripting.effects.DrawCardsEffect(1)",
            "cost = AdditionalCost.Sacrifice()","trigger = TriggerSpec()","amount = DynamicAmount.Fixed(2)",
            "storeAs = \"cards\"", "effect = Effects.If(Gate.WhenCondition(c), e)").forEach {
            authoringViolations(it).isNotEmpty() shouldBe true
        }
        authoringViolations("// DrawCardsEffect(1)\nimport foo.DrawCardsEffect\neffect = Effects.DrawCards(1)") shouldBe emptyList()
    }
})

// Focused source-text guard, adapted from upstream FacadeBoundaryTest. CardValidator owns semantic lint.
internal fun authoringViolations(source: String): List<String> {
    val code=source.replace(Regex("/\\*.*?\\*/",RegexOption.DOT_MATCHES_ALL),"").lineSequence()
        .filterNot { it.trimStart().startsWith("import ") }.map { it.substringBefore("//") }.joinToString("\n")
    val forbidden=listOf(
        Regex("""(?<![\w.])[A-Z]\w*Effect\s*\("""),
        Regex("""\bscripting\.effects\.[A-Z]\w*Effect\s*\("""),
        Regex("""\b(AdditionalCost|PayCost|DynamicAmount)\.[A-Z]"""),
        Regex("""\b(TriggerSpec|GatedEffect)\s*\(|\b(Gate|TriggerBinding)\."""),
        Regex("""\b(storeAs|storeSelected|storeRemainder|collectionName|from)\s*=\s*""""),
        Regex("""\bVariableReference\s*\(\s*""""),
    )
    return forbidden.flatMap { pattern -> pattern.findAll(code).map { it.value }.toList() }
}
