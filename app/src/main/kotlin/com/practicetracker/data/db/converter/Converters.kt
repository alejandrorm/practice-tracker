package com.practicetracker.data.db.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
    @TypeConverter fun toInstant(instant: Instant?): Long? = instant?.toEpochMilli()
    @TypeConverter fun fromLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
    @TypeConverter fun toLocalDate(date: LocalDate?): String? = date?.toString()
}
