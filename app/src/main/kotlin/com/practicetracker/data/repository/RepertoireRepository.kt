package com.practicetracker.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads violin (or any instrument) repertoire data from
 * `assets/repertoire/{instrument}/repertoire.json` and
 * `assets/repertoire/{instrument}/practice.json`.
 *
 * If the folder for an instrument doesn't exist the repository silently returns
 * empty lists, so new instruments can be added by simply dropping their files
 * in the appropriate subfolder — no code changes required.
 */
@Singleton
class RepertoireRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class RepertoireEntry(
        val name: String,
        val composer: String,
        val level: Int,
        val levelAlias: String,
        val style: String,
        val suggestedPractice: List<String>
    )

    private val repertoireCache = ConcurrentHashMap<String, List<RepertoireEntry>>()
    private val practiceCache = ConcurrentHashMap<String, List<String>>()

    private fun normalize(instrument: String) =
        instrument.lowercase().trim().replace(Regex("\\s+"), "_")

    private fun loadRepertoire(folder: String): List<RepertoireEntry> = try {
        val json = context.assets.open("repertoire/$folder/repertoire.json")
            .bufferedReader().readText()
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val practiceArray = obj.getJSONArray("Suggested_practice")
            RepertoireEntry(
                name = obj.getString("Name"),
                composer = obj.getString("Composer"),
                level = obj.getInt("Level"),
                levelAlias = obj.getString("Level_alias"),
                style = obj.getString("Style"),
                suggestedPractice = (0 until practiceArray.length()).map { j ->
                    practiceArray.getString(j)
                }
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun loadPracticeItems(folder: String): List<String> = try {
        val json = context.assets.open("repertoire/$folder/practice.json")
            .bufferedReader().readText()
        val array = JSONArray(json)
        (0 until array.length()).map { i -> array.getString(i) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun repertoireFor(instrument: String): List<RepertoireEntry> {
        val folder = normalize(instrument)
        return repertoireCache.computeIfAbsent(folder) { loadRepertoire(it) }
    }

    private fun practiceFor(instrument: String): List<String> {
        val folder = normalize(instrument)
        return practiceCache.computeIfAbsent(folder) { loadPracticeItems(it) }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Maps a user skill level string to the corresponding repertoire level range. */
    fun levelRangeForSkillLevel(skillLevel: String): IntRange = when (skillLevel.lowercase()) {
        "beginner"     -> 1..2
        "intermediate" -> 3..5
        "advanced"     -> 6..7
        "professional" -> 1..8
        else           -> 1..8
    }

    fun findByTitle(title: String, instrument: String): RepertoireEntry? =
        repertoireFor(instrument).find { it.name.equals(title, ignoreCase = true) }

    /**
     * Returns repertoire entries matching [query] for [instrument], sorted so
     * that entries in the user's skill level range appear first.
     */
    fun searchRepertoire(query: String, instrument: String, skillLevel: String): List<RepertoireEntry> {
        if (query.length < 2) return emptyList()
        val range = levelRangeForSkillLevel(skillLevel)
        return repertoireFor(instrument)
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedWith(
                compareByDescending<RepertoireEntry> { it.level in range }.thenBy { it.level }
            )
    }

    /** Searches `practice.json` items for [instrument] by partial match. */
    fun searchPracticeItems(query: String, instrument: String): List<String> {
        if (query.length < 2) return emptyList()
        return practiceFor(instrument).filter { it.contains(query, ignoreCase = true) }
    }
}
