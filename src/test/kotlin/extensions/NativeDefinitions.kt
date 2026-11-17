package dev.mtg.assistant.extensions

import dev.mtg.assistant.Catalog

// Existing behavioral scenarios now execute the active native definitions, not retired copies.
private val adoptedCatalog by lazy { Catalog() }
val adoptedNativeDefinitions get() = listOf(SpinedTyrranax, MightOfMurasa, LabyrinthOfSkophos,
    GaladhrimAmbush, HourOfPromise, ThroneOfMakindi, PlanewideCelebration, TyriteSanctum,
    KrenkosCommand, Bitterblossom, JudithTheScourgeDiva, OviyaPashiriSageLifecrafter)
val SpinedTyrranax get() = adoptedCatalog.card("Spined Tyrranax")!!
val MightOfMurasa get() = adoptedCatalog.card("Might of Murasa")!!
val LabyrinthOfSkophos get() = adoptedCatalog.card("Labyrinth of Skophos")!!
val GaladhrimAmbush get() = adoptedCatalog.card("Galadhrim Ambush")!!
val HourOfPromise get() = adoptedCatalog.card("Hour of Promise")!!
val ThroneOfMakindi get() = adoptedCatalog.card("Throne of Makindi")!!
val PlanewideCelebration get() = adoptedCatalog.card("Planewide Celebration")!!
val TyriteSanctum get() = adoptedCatalog.card("Tyrite Sanctum")!!
val KrenkosCommand get() = adoptedCatalog.card("Krenko's Command")!!
val Bitterblossom get() = adoptedCatalog.card("Bitterblossom")!!
val JudithTheScourgeDiva get() = adoptedCatalog.card("Judith, the Scourge Diva")!!
val OviyaPashiriSageLifecrafter get() = adoptedCatalog.card("Oviya Pashiri, Sage Lifecrafter")!!
