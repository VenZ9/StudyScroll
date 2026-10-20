package app.studyscroll.ui.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studyscroll.data.repository.DocumentRepository
import app.studyscroll.domain.model.Document
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HomeUiEvent {
    data class NavigateToInspect(val documentId: String) : HomeUiEvent()
    data class NavigateToFeed(val documentId: String) : HomeUiEvent()
    data class ShowError(val message: String) : HomeUiEvent()
}

class HomeViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    val documents: StateFlow<List<Document>> = documentRepository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow("")
    val importProgress: StateFlow<String> = _importProgress.asStateFlow()

    private val _uiEvents = MutableSharedFlow<HomeUiEvent>()
    val uiEvents: SharedFlow<HomeUiEvent> = _uiEvents.asSharedFlow()

    fun handlePdfPicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = "Validating PDF size..."

            try {
                var fileName = "Document.pdf"
                var fileSize = -1L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                _importProgress.value = "Extracting text locally..."
                val result = documentRepository.importPdf(
                    fileName = fileName,
                    fileSizeBytes = fileSize,
                    inputStreamProvider = { context.contentResolver.openInputStream(uri) }
                )

                if (result.isSuccess) {
                    val doc = result.getOrThrow()
                    _uiEvents.emit(HomeUiEvent.NavigateToInspect(doc.id))
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to import PDF."
                    _uiEvents.emit(HomeUiEvent.ShowError(err))
                }
            } catch (e: Exception) {
                _uiEvents.emit(HomeUiEvent.ShowError(e.message ?: "Failed to process PDF file."))
            } finally {
                _isImporting.value = false
                _importProgress.value = ""
            }
        }
    }

    fun renameDocument(documentId: String, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                documentRepository.renameDocument(documentId, newTitle.trim())
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId)
        }
    }
}
