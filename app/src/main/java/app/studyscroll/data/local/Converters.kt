package app.studyscroll.data.local

import androidx.room.TypeConverter
import app.studyscroll.domain.model.ConfidenceLabel
import app.studyscroll.domain.model.LessonType
import app.studyscroll.domain.model.SourceReference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromLessonType(type: LessonType): String = type.name

    @TypeConverter
    fun toLessonType(value: String): LessonType = runCatching {
        LessonType.valueOf(value)
    }.getOrDefault(LessonType.KEY_FACT)

    @TypeConverter
    fun fromConfidenceLabel(confidence: ConfidenceLabel): String = confidence.name

    @TypeConverter
    fun toConfidenceLabel(value: String): ConfidenceLabel = runCatching {
        ConfidenceLabel.valueOf(value)
    }.getOrDefault(ConfidenceLabel.HIGH)

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(value)
    }.getOrDefault(emptyList())

    @TypeConverter
    fun fromSourceReferenceList(list: List<SourceReference>): String = json.encodeToString(list)

    @TypeConverter
    fun toSourceReferenceList(value: String): List<SourceReference> = runCatching {
        json.decodeFromString<List<SourceReference>>(value)
    }.getOrDefault(emptyList())
}
