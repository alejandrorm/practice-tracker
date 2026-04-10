package com.practicetracker.data.repository

import android.content.Context
import com.practicetracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

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

    private val _repertoire: List<RepertoireEntry> by lazy { loadRepertoire() }
    private val _practiceItems: List<String> by lazy { loadPracticeItems() }

    private fun loadRepertoire(): List<RepertoireEntry> {
        val json = context.resources.openRawResource(R.raw.violin_repertoire).bufferedReader().readText()
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
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
    }

    private fun loadPracticeItems(): List<String> {
        val json = context.resources.openRawResource(R.raw.violin_practice).bufferedReader().readText()
        val array = JSONArray(json)
        return (0 until array.length()).map { i -> array.getString(i) }
    }

    /** Maps a user skill level string to the corresponding repertoire level range. */
    fun levelRangeForSkillLevel(skillLevel: String): IntRange = when (skillLevel.lowercase()) {
        "beginner" -> 1..2
        "intermediate" -> 3..5
        "advanced" -> 6..7
        "professional" -> 1..8
        else -> 1..8
    }

    fun getRepertoire(): List<RepertoireEntry> = _repertoire

    fun getPracticeItems(): List<String> = _practiceItems

    fun findByTitle(title: String): RepertoireEntry? =
        _repertoire.find { it.name.equals(title, ignoreCase = true) }

    /**
     * Returns repertoire entries matching [query], sorted so that entries in the
     * user's skill level range appear first.
     */
    fun searchRepertoire(query: String, skillLevel: String): List<RepertoireEntry> {
        if (query.length < 2) return emptyList()
        val range = levelRangeForSkillLevel(skillLevel)
        return _repertoire
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedWith(
                compareByDescending<RepertoireEntry> { it.level in range }.thenBy { it.level }
            )
    }

    /** Searches [violin_practice.json] items by partial match. */
    fun searchPracticeItems(query: String): List<String> {
        if (query.length < 2) return emptyList()
        return _practiceItems.filter { it.contains(query, ignoreCase = true) }
    }
}
