package com.bayg.services.storage

import androidx.room.TypeConverter
import com.bayg.services.storage.entities.BlockEventSeverity

class Converters {
    @TypeConverter
    fun fromSeverity(value: BlockEventSeverity): String = value.name

    @TypeConverter
    fun toSeverity(value: String): BlockEventSeverity =
        runCatching { BlockEventSeverity.valueOf(value) }.getOrDefault(BlockEventSeverity.RED)
}
