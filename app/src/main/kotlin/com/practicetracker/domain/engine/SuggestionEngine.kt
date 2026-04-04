package com.practicetracker.domain.engine

import com.practicetracker.domain.model.PieceType

object SuggestionEngine {

    fun suggestSkills(pieceType: PieceType, instrument: String): List<String> {
        return when (pieceType) {
            PieceType.SCALE -> listOf(
                "Intonation", "Bow speed", "Tone quality", "Rhythm", "Dynamics", "Even fingers"
            )
            PieceType.ETUDE -> listOf(
                "Intonation", "Bow pressure", "Rhythm", "Articulation", "Shifting", "Dynamics"
            )
            PieceType.SONG, PieceType.CONCERTO -> listOf(
                "Intonation", "Tone quality", "Vibrato", "Phrasing",
                "Bow distribution", "Shifting", "Dynamics", "Memorization"
            )
            PieceType.EXERCISE -> listOf(
                "Bow pressure", "Finger placement", "Wrist flexibility", "Even tempo"
            )
            PieceType.OTHER -> listOf("Intonation", "Rhythm", "Tone quality")
        }
    }

    fun suggestScales(instrument: String, pieceTitles: List<String>): List<String> {
        val lowerInstrument = instrument.lowercase()
        val scales: List<String> = when {
            lowerInstrument.contains("piano") -> listOf(
                "C Major Scale", "G Major Scale", "D Major Scale", "A Major Scale",
                "E Major Scale", "B Major Scale", "F Major Scale", "Bb Major Scale",
                "Eb Major Scale", "Ab Major Scale", "Db Major Scale", "F# Major Scale"
            )
            lowerInstrument.contains("guitar") -> listOf(
                "E Major Scale", "A Major Scale", "D Major Scale", "G Major Scale",
                "C Major Scale", "B Major Scale", "F# Major Scale", "F Major Scale",
                "Bb Major Scale", "Eb Major Scale", "Ab Major Scale", "Db Major Scale"
            )
            else -> listOf(
                "G Major Scale", "D Major Scale", "A Major Scale", "E Major Scale",
                "B Major Scale", "F# Major Scale", "C Major Scale", "F Major Scale",
                "Bb Major Scale", "Eb Major Scale", "Ab Major Scale", "Db Major Scale"
            )
        }
        val lowerTitles = pieceTitles.map { it.lowercase() }
        return scales.filter { scale -> !lowerTitles.contains(scale.lowercase()) }
    }
}
