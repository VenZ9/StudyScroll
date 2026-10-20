package app.studyscroll.ui.importpdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studyscroll.data.ai.ProviderManager
import app.studyscroll.data.local.dao.PageTextDao
import app.studyscroll.data.pdf.BlockType
import app.studyscroll.data.pdf.ExtractedPage
import app.studyscroll.data.pdf.TextSegmenter
import app.studyscroll.data.repository.DocumentRepository
import app.studyscroll.data.repository.LessonRepository
import app.studyscroll.data.repository.SettingsRepository
import app.studyscroll.domain.model.Document
import app.studyscroll.domain.model.ProviderProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InspectUiEvent {
    data class GenerationSuccess(val count: Int) : InspectUiEvent()
    data class ShowError(val message: String) : InspectUiEvent()
    object RequestCloudConsent : InspectUiEvent()
}

class PdfInspectViewModel(
    private val documentId: String,
    private val documentRepository: DocumentRepository,
    private val lessonRepository: LessonRepository,
    private val providerManager: ProviderManager,
    private val pageTextDao: PageTextDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _document = MutableStateFlow<Document?>(null)
    val document: StateFlow<Document?> = _document.asStateFlow()

    private val _detectedHeadings = MutableStateFlow<List<String>>(emptyList())
    val detectedHeadings: StateFlow<List<String>> = _detectedHeadings.asStateFlow()

    private val _sampleExcerpt = MutableStateFlow("")
    val sampleExcerpt: StateFlow<String> = _sampleExcerpt.asStateFlow()

    val providers: StateFlow<List<ProviderProfile>> = providerManager.allProviders
        .stateInScope(viewModelScope, emptyList())

    private val _selectedProviderId = MutableStateFlow("provider_offline")
    val selectedProviderId: StateFlow<String> = _selectedProviderId.asStateFlow()

    private val _isAllPages = MutableStateFlow(true)
    val isAllPages: StateFlow<Boolean> = _isAllPages.asStateFlow()

    private val _startPage = MutableStateFlow(1)
    val startPage: StateFlow<Int> = _startPage.asStateFlow()

    private val _endPage = MutableStateFlow(1)
    val endPage: StateFlow<Int> = _endPage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _uiEvents = MutableSharedFlow<InspectUiEvent>()
    val uiEvents: SharedFlow<InspectUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val doc = documentRepository.getDocument(documentId)
            _document.value = doc

            if (doc != null) {
                _endPage.value = doc.pageCount.coerceAtLeast(1)

                val texts = pageTextDao.getPageTextsForDocument(documentId)
                if (texts.isNotEmpty()) {
                    val sample = texts.firstOrNull { it.text.isNotBlank() }?.text ?: ""
                    _sampleExcerpt.value = sample.take(300)

                    val extractedPages = texts.map { ExtractedPage(it.pageNumber, it.text) }
                    val blocks = TextSegmenter.segmentPages(extractedPages)
                    val headings = blocks.filter { it.type == BlockType.HEADING }.map { it.title }.take(10)
                    _detectedHeadings.value = headings
                }
            }

            val preferred = providerManager.getPreferredProvider()
            _selectedProviderId.value = preferred.id
        }
    }

    fun selectProvider(providerId: String) {
        _selectedProviderId.value = providerId
    }

    fun setAllPages(all: Boolean) {
        _isAllPages.value = all
    }

    fun setPageRange(start: Int, end: Int) {
        _startPage.value = start
        _endPage.value = end
    }

    fun grantCloudConsentAndGenerate() {
        viewModelScope.launch {
            settingsRepository.setCloudConsent(true)
            generateLessons()
        }
    }

    fun generateLessons() {
        viewModelScope.launch {
            val providerId = _selectedProviderId.value
            val isOffline = providerId == "provider_offline"

            if (!isOffline) {
                val settings = settingsRepository.getSettings()
                if (!settings.cloudProcessingConsented) {
                    _uiEvents.emit(InspectUiEvent.RequestCloudConsent)
                    return@launch
                }
            }

            _isGenerating.value = true
            val pageRange = if (!_isAllPages.value) {
                Pair(_startPage.value, _endPage.value)
            } else null

            val result = lessonRepository.generateLessons(documentId, providerId, pageRange)
            _isGenerating.value = false

            if (result.isSuccess) {
                _uiEvents.emit(InspectUiEvent.GenerationSuccess(result.getOrThrow()))
            } else {
                val err = result.exceptionOrNull()?.message ?: "Generation failed."
                _uiEvents.emit(InspectUiEvent.ShowError(err))
            }
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInScope(
        scope: kotlinx.coroutines.CoroutineScope,
        initialValue: T
    ): StateFlow<T> = this.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), initialValue)
}
