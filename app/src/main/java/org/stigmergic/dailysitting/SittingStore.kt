package org.stigmergic.dailysitting

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val PREFERENCES_NAME = "daily_sitting"
private const val KEY_PRESETS = "presets"
private const val KEY_SESSIONS = "sessions"

class SittingStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        preferences.edit().remove(KEY_SESSIONS).apply()
    }

    fun loadPresets(): List<TimerPreset> {
        val stored = preferences.getString(KEY_PRESETS, null)
        if (stored.isNullOrBlank()) {
            return defaultPresets().also(::savePresets)
        }

        return try {
            parsePresets(stored).ifEmpty { defaultPresets().also(::savePresets) }
        } catch (_: JSONException) {
            defaultPresets().also(::savePresets)
        }
    }

    fun savePreset(preset: TimerPreset) {
        val updated = loadPresets()
            .filterNot { it.id == preset.id }
            .plus(preset)
            .sortedBy { it.durationMinutes }
        savePresets(updated)
    }

    fun deletePreset(presetId: String) {
        savePresets(loadPresets().filterNot { it.id == presetId })
    }

    private fun savePresets(presets: List<TimerPreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject()
                    .put("id", preset.id)
                    .put("name", preset.name)
                    .put("durationMinutes", preset.durationMinutes)
                    .put("intervalMinutes", preset.intervalMinutes ?: JSONObject.NULL)
                    .put("bellSoundId", cleanBellSoundId(preset.bellSoundId)),
            )
        }
        preferences.edit().putString(KEY_PRESETS, array.toString()).apply()
    }

    private fun parsePresets(json: String): List<TimerPreset> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    TimerPreset(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        durationMinutes = item.getInt("durationMinutes"),
                        intervalMinutes = if (item.has("intervalMinutes") && !item.isNull("intervalMinutes")) {
                            item.getInt("intervalMinutes")
                        } else {
                            null
                        },
                        bellSoundId = cleanBellSoundId(item.optString("bellSoundId", DefaultBellSoundId)),
                    ),
                )
            }
        }
    }

    private fun defaultPresets(): List<TimerPreset> = listOf(
        TimerPreset(
            id = "default-10",
            name = "10 minutes",
            durationMinutes = 10,
            intervalMinutes = null,
        ),
        TimerPreset(
            id = "default-20",
            name = "20 minutes",
            durationMinutes = 20,
            intervalMinutes = 5,
        ),
        TimerPreset(
            id = "default-30",
            name = "30 minutes",
            durationMinutes = 30,
            intervalMinutes = 10,
        ),
    )
}
