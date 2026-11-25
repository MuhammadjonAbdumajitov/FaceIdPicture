package com.example.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String {
        if (value == null || value.isEmpty()) return ""
        return value.joinToString(separator = ",")
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray {
        if (value.isNullOrBlank()) return FloatArray(0)
        return value.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .toFloatArray()
    }
}
