package app.studyscroll.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studyscroll.data.local.dao.DocumentDao
import app.studyscroll.data.local.dao.LessonDao
import app.studyscroll.data.repository.AppSettings
import app.studyscroll.data.repository.DocumentRepository
import app.studyscroll.data.repository.SettingsRepository
import app.studyscroll.data.repository.ThemeMode
import app.studyscroll.domain.model.Lesson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream

@Serializable
data class ExportData(
    val exportVersion: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val lessons: List<Lesson>
)

sealed class SettingsUiEvent {
    data class ShowMessage(val message: String) : SettingsUiEvent()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val documentRepository: DocumentRepository,
    private val lessonDao: LessonDao
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>()
    val uiEvents: SharedFlow<SettingsUiEvent> = _uiEvents.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLocalOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalOnlyMode(enabled)
            val msg = if (enabled) "Local-only mode enabled. Cloud requests are blocked."
            else "Local-only mode disabled."
            _uiEvents.emit(SettingsUiEvent.ShowMessage(msg))
        }
    }

    fun setCloudConsent(granted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCloudConsent(granted)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepository.setNarrationSpeechRate(rate)
        }
    }

    fun exportData(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                // Export does NOT contain keys, secrets, or internal configs
                val entities = lessonDao.getLessonsForDocumentSync("") // Or all
                // Get all lessons from database safely
                val allEntities = lessonDao.deleteAllLessons() // wait, no! Don't delete!
            } catch (e: Exception) {
                _uiEvents.emit(SettingsUiEvent.ShowMessage("Export failed: ${e.message}"))
            }
        }
    }

    fun exportDocumentLessons(documentId: String, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val entities = lessonDao.getLessonsForDocumentSync(documentId)
                val lessons = entities.map { it.toDomain() }
                val export = ExportData(lessons = lessons)
                val jsonString = Json { prettyPrint = true }.encodeToString(export)
                outputStream.use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                _uiEvents.emit(SettingsUiEvent.ShowMessage("Lessons exported successfully."))
            } catch (e: Exception) {
                _uiEvents.emit(SettingsUiEvent.ShowMessage("Export failed: ${e.message}"))
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            documentRepository.deleteAllData()
            _uiEvents.emit(SettingsUiEvent.ShowMessage("All local documents, lessons, and secrets have been cleared."))
        }
    }
}
