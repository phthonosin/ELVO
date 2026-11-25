package com.elvo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.elvo.app.scheduler.WeekPlanner
import com.elvo.app.scheduler.WeekReminder
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elvo_preferences")

class PreferencesRepository(private val context: Context) {

    companion object {
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val BLOCK_UNINSTALL = booleanPreferencesKey("block_uninstall")
        val SELECTED_BLOCK_MODE = stringPreferencesKey("selected_block_mode")
        val ACTIVE_PROMPT_TEXT = stringPreferencesKey("active_prompt_text")
        val MASTER_CODE = stringPreferencesKey("master_code")
        val BLOCK_COUNTS = stringPreferencesKey("block_counts")
        val WEEK_PLAN_JSON = stringPreferencesKey("week_plan_json")
        val WEEK_DESCRIPTION = stringPreferencesKey("week_description")
    }

    val setupCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SETUP_COMPLETED] ?: false
    }

    val blockUninstall: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BLOCK_UNINSTALL] ?: true
    }

    val selectedBlockMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_BLOCK_MODE] ?: "VIDEO"
    }

    val activePromptText: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_PROMPT_TEXT] ?: ""
    }

    val masterCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[MASTER_CODE] ?: "k7#Qm2\$xP9!w"
    }

    val weekPlan: Flow<List<WeekReminder>> = context.dataStore.data.map { prefs ->
        WeekPlanner.fromJson(prefs[WEEK_PLAN_JSON] ?: "[]")
    }

    val weekDescription: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[WEEK_DESCRIPTION] ?: ""
    }

    /** Block counts keyed by yyyy-MM-dd, kept for the last seven days. */
    val blockCounts: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        parseBlockCounts(prefs[BLOCK_COUNTS] ?: "")
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs -> prefs[SETUP_COMPLETED] = completed }
    }

    suspend fun setBlockUninstall(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BLOCK_UNINSTALL] = enabled }
    }

    suspend fun setSelectedBlockMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_BLOCK_MODE] = mode }
    }

    suspend fun setActivePromptText(prompt: String) {
        context.dataStore.edit { prefs -> prefs[ACTIVE_PROMPT_TEXT] = prompt }
    }

    suspend fun setMasterCode(code: String) {
        context.dataStore.edit { prefs -> prefs[MASTER_CODE] = code }
    }

    suspend fun saveWeekPlan(description: String, plan: List<WeekReminder>) {
        context.dataStore.edit { prefs ->
            prefs[WEEK_DESCRIPTION] = description
            prefs[WEEK_PLAN_JSON] = WeekPlanner.toJson(plan)
        }
    }

    /** Marks one task done (or undone) without touching the rest of the plan. */
    suspend fun setWeekTaskDone(day: Int, index: Int, done: Boolean) {
        context.dataStore.edit { prefs ->
            val plan = WeekPlanner.fromJson(prefs[WEEK_PLAN_JSON] ?: "[]")
                .map { entry ->
                    if (entry.dayOfWeek == day) entry.withTaskDone(index, done) else entry
                }
            prefs[WEEK_PLAN_JSON] = WeekPlanner.toJson(plan)
        }
    }

    suspend fun recordBlockEvent() {
        val today = LocalDate.now()
        val cutoff = today.minusDays(6)
        context.dataStore.edit { prefs ->
            val counts = parseBlockCounts(prefs[BLOCK_COUNTS] ?: "").toMutableMap()
            counts[today.toString()] = (counts[today.toString()] ?: 0) + 1
            counts.keys.removeAll { key ->
                runCatching { LocalDate.parse(key).isBefore(cutoff) }.getOrDefault(true)
            }
            prefs[BLOCK_COUNTS] = counts.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    private fun parseBlockCounts(raw: String): Map<String, Int> =
        raw.split(',')
            .mapNotNull { entry ->
                val parts = entry.split(':', limit = 2)
                val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                parts[0] to count
            }
            .toMap()
}
