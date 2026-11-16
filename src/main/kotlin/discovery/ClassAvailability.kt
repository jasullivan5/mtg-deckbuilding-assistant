package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ClassLevelAbility
import com.wingedsheep.sdk.scripting.TimingRule
import kotlinx.serialization.Serializable

@Serializable data class ClassAdvance(val level: Int, val path: String, val manaCost: ManaCost)

/** The unlock belongs to the source permanent, independently of the ability's own cost and timing. */
@Serializable data class ClassAvailability(
    val minimumLevel: Int,
    val levelPath: String,
    val progression: List<ClassAdvance>,
    val sourceZone: Zone = Zone.BATTLEFIELD,
    val levelUpTiming: TimingRule = TimingRule.SorcerySpeed,
    val support: Verdict
) {
    fun atLevel(currentLevel: Int?): Verdict = when {
        currentLevel != null && currentLevel < minimumLevel -> Verdict.NO_MATCH
        else -> support
    }

    fun prerequisites(): List<String> = listOf(
        "Source must be on the battlefield at Class level $minimumLevel or higher.",
        "Advance at sorcery speed, paying each unlock once per progression: " +
            progression.joinToString { "level ${it.level} for ${it.manaCost}" } + "."
    )
}

internal fun classAvailability(levels: List<ClassLevelAbility>, index: Int, isClass: Boolean): ClassAvailability {
    val minimum = levels[index].level
    val progression = levels.mapIndexedNotNull { i, level ->
        ClassAdvance(level.level, "script.classLevels[$i]", level.cost).takeIf { level.level <= minimum }
    }.sortedBy { it.level }
    val established = isClass && minimum in 2..3 && progression.map { it.level } == (2..minimum).toList()
    return ClassAvailability(minimum, "script.classLevels[$index]", progression,
        support = if (established) Verdict.MATCH else Verdict.UNKNOWN)
}
