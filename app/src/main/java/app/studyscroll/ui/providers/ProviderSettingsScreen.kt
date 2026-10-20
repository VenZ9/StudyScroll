package app.studyscroll.ui.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import app.studyscroll.ui.components.ConfirmDialog
import app.studyscroll.ui.components.StudyScrollTopBar
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    viewModel: ProviderSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val providers by viewModel.providers.collectAsState()
    val testingId by viewModel.testingProviderId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingProvider by remember { mutableStateOf<ProviderProfile?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf<ProviderProfile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is ProviderUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            StudyScrollTopBar(
                title = "AI Providers",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Security notice: API keys are stored locally using Android Keystore-backed AES-GCM encryption. Keys are never transmitted except directly to the designated provider endpoint.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(providers, key = { it.id }) { provider ->
                ProviderCard(
                    profile = provider,
                    isTesting = testingId == provider.id,
                    onSetPreferred = { viewModel.setPreferred(provider.id) },
                    onEdit = { editingProvider = provider },
                    onTest = { viewModel.testConnection(provider.id) },
                    onDelete = { providerToDelete = provider }
                )
            }

            item {
                OutlinedButton(
                    onClick = { isCreatingNew = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Provider")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (editingProvider != null || isCreatingNew) {
        val target = editingProvider
        ProviderEditDialog(
            profile = target,
            onDismiss = {
                editingProvider = null
                isCreatingNew = false
            },
            onSave = { type, displayName, baseUrl, modelId, authMode, key ->
                viewModel.saveProvider(
                    id = target?.id,
                    type = type,
                    displayName = displayName,
                    baseUrl = baseUrl,
                    modelId = modelId,
                    authMode = authMode,
                    apiKey = key
                )
                editingProvider = null
                isCreatingNew = false
            }
        )
    }

    providerToDelete?.let { profile ->
        ConfirmDialog(
            title = "Remove Provider?",
            message = "Remove \"${profile.displayName}\"? Any saved API key for this provider will be permanently deleted from secure storage.",
            confirmButtonText = "Remove",
            onConfirm = {
                viewModel.deleteProvider(profile.id)
                providerToDelete = null
            },
            onDismiss = { providerToDelete = null }
        )
    }
}

@Composable
fun ProviderCard(
    profile: ProviderProfile,
    isTesting: Boolean,
    onSetPreferred: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (profile.isPreferred) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Preferred",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (profile.type == ProviderType.OFFLINE) "100% Offline (No key needed)"
                        else "Model: ${profile.modelId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (profile.requiresApiKey) {
                        Text(
                            text = if (profile.hasApiKey) "API Key: Configured (Encrypted)" else "API Key: Not configured",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.hasApiKey) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    } else if (profile.authMode == AuthMode.ANONYMOUS_FREE) {
                        Text(
                            text = "Auth: Anonymous free access (Rate limits apply)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row {
                    if (profile.id != "provider_offline") {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Provider",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (profile.id.startsWith("custom_")) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Provider",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!profile.isPreferred) {
                    OutlinedButton(
                        onClick = onSetPreferred,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Preferred")
                    }
                }

                if (profile.type != ProviderType.OFFLINE) {
                    Button(
                        onClick = onTest,
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderEditDialog(
    profile: ProviderProfile?,
    onDismiss: () -> Unit,
    onSave: (ProviderType, String, String, String, AuthMode, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
    var baseUrl by remember { mutableStateOf(profile?.baseUrl ?: "") }
    var modelId by remember { mutableStateOf(profile?.modelId ?: "") }
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    val type = profile?.type ?: ProviderType.CUSTOM
    val authMode = profile?.authMode ?: AuthMode.API_KEY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (profile == null) "Add Custom Provider" else "Edit ${profile.displayName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (authMode == AuthMode.API_KEY) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(if (profile?.hasApiKey == true) "New API Key (Leave blank to keep)" else "API Key") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Hide" else "Show"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(type, displayName, baseUrl, modelId, authMode, apiKey.takeIf { it.isNotBlank() })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
