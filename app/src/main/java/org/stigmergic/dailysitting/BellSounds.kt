// SPDX-License-Identifier: GPL-3.0-only

package org.stigmergic.dailysitting

const val DefaultBellSoundId = "struck"

enum class BellSoundIcon {
    Bowl,
    Ring,
    Bell,
    Temple,
    Harmony,
}

data class BellSoundOption(
    val id: String,
    val name: String,
    val description: String,
    val rawResId: Int,
    val icon: BellSoundIcon,
)

val BellSoundOptions = listOf(
    BellSoundOption(
        id = DefaultBellSoundId,
        name = "Soft strike",
        description = "Clear bowl hit",
        rawResId = R.raw.bowl_struck,
        icon = BellSoundIcon.Bell,
    ),
    BellSoundOption(
        id = "rubbed",
        name = "Warm bowl",
        description = "Rubbed resonance",
        rawResId = R.raw.bowl_rubbed,
        icon = BellSoundIcon.Ring,
    ),
    BellSoundOption(
        id = "bright",
        name = "Bright bowl",
        description = "Short bright chime",
        rawResId = R.raw.bowl_bright,
        icon = BellSoundIcon.Bowl,
    ),
    BellSoundOption(
        id = "temple",
        name = "Temple bowl",
        description = "Low temple tone",
        rawResId = R.raw.bowl_temple,
        icon = BellSoundIcon.Temple,
    ),
    BellSoundOption(
        id = "harmony",
        name = "Harmony",
        description = "Layered bowls",
        rawResId = R.raw.bowl_harmony,
        icon = BellSoundIcon.Harmony,
    ),
)

fun bellSoundForId(id: String?): BellSoundOption =
    BellSoundOptions.firstOrNull { it.id == id } ?: BellSoundOptions.first()

fun cleanBellSoundId(id: String?): String = bellSoundForId(id).id
