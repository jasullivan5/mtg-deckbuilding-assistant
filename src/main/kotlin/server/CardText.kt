package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable

@Serializable data class RulesLine(val text: String, val actionId: String? = null, val relevance: Verdict? = null)
internal data class RulesLink(val actionId: String, val face: String?, val relevance: Verdict, val candidates: List<String>)

/** Exact whole-line display association only. No game facts or verdicts are inferred from card text. */
fun rulesLines(name: String, text: String, abilities: List<AbilityInspection>): List<RulesLine> {
    return linkedRulesLines(name, text, abilities.map { RulesLink(it.actionId, it.face, it.verdict, it.textCandidates) })
}
internal fun linkedRulesLines(name: String, text: String, links: List<RulesLink>): List<RulesLine> {
    fun key(value: String): String {
        var normalized = value.lowercase().trim().replace(Regex("\\s+"), " ")
        for (alias in listOf(name, name.substringBefore(',')).distinct().sortedByDescending { it.length }) {
            normalized = normalized.replace(Regex("(?<![\\p{L}\\p{N}])${Regex.escape(alias.lowercase())}(?![\\p{L}\\p{N}])"), "self")
        }
        return normalized.replace("this source", "self").replace("this permanent", "self").replace("this creature", "self")
    }
    val lines = text.lines().filter { it.isNotBlank() }
    return lines.map { line ->
        val normalized = key(line)
        val candidates = links.filter { it.face == null && it.relevance != Verdict.NO_MATCH && it.candidates.any { candidate -> key(candidate) == normalized } }
        val ability = candidates.singleOrNull()?.takeIf { lines.count { key(it) == normalized } == 1 }
        RulesLine(line, ability?.actionId, ability?.relevance)
    }
}
