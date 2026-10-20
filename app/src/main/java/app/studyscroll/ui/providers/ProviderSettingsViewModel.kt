package app.studyscroll.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studyscroll.data.ai.ProviderManager
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ProviderUiEvent {
    data class ShowMessage(val message: String) : ProviderUiEvent()
}

class ProviderSettingsViewModel(
    private val providerManager: ProviderManager
) : ViewModel() {

    val providers: StateFlow<List<ProviderProfile>> = providerManager.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testingProviderId = MutableStateFlow<String?>(null)
    val testingProviderId: StateFlow<String?> = _testingProviderId.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ProviderUiEvent>()
    val uiEvents: SharedFlow<ProviderUiEvent> = _uiEvents.asSharedFlow()

    fun setPreferred(providerId: String) {
        viewModelScope.launch {
            providerManager.setPreferredProvider(providerId)
            _uiEvents.emit(ProviderUiEvent.ShowMessage("Preferred provider updated."))
        }
    }

    fun testConnection(providerId: String, apiKeyOverride: String? = null) {
        viewModelScope.launch {
            _testingProviderId.value = providerId
            val result = providerManager.testConnection(providerId, apiKeyOverride)
            _testingProviderId.value = null

            if (result.isSuccess) {
                _uiEvents.emit(ProviderUiEvent.ShowMessage(result.getOrThrow()))
            } else {
                val err = result.exceptionOrNull()?.message ?: "Connection test failed."
                _uiEvents.emit(ProviderUiEvent.ShowMessage(err))
            }
        }
    }

    fun saveProvider(
        id: String?,
        type: ProviderType,
        displayName: String,
        baseUrl: String,
        modelId: String,
        authMode: AuthMode,
        apiKey: String?
    ) {
        viewModelScope.launch {
            val providerId = id ?: "custom_${UUID.randomUUID()}"
            val profile = ProviderProfile(
                id = providerId,
                type = type,
                displayName = displayName.ifBlank { type.name },
                baseUrl = baseUrl.trim(),
                modelId = modelId.trim(),
                authMode = authMode,
                isPreferred = false
            )
            providerManager.saveProvider(profile, apiKey?.takeIf { it.isNotBlank() })
            _uiEvents.emit(ProviderUiEvent.ShowMessage("Provider saved successfully."))
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            providerManager.deleteProvider(providerId)
            _uiEvents.emit(ProviderUiEvent.ShowMessage("Provider removed."))
        }
    }
}
