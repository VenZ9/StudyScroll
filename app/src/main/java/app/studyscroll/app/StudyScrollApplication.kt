package app.studyscroll.app

import android.app.Application
import app.studyscroll.data.ai.ProviderManager
import app.studyscroll.data.local.StudyScrollDatabase
import app.studyscroll.data.pdf.PdfBoxTextExtractor
import app.studyscroll.data.repository.DocumentRepository
import app.studyscroll.data.repository.LessonRepository
import app.studyscroll.data.repository.SettingsRepository
import app.studyscroll.data.repository.DataStoreSettingsRepository
import app.studyscroll.data.security.KeystoreSecretStorage
import app.studyscroll.data.security.SecretStorage
import app.studyscroll.narration.NarrationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyScrollApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { StudyScrollDatabase.getInstance(this) }
    val secretStorage: SecretStorage by lazy { KeystoreSecretStorage(this) }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(this) }
    val pdfTextExtractor by lazy { PdfBoxTextExtractor(this) }

    val providerManager by lazy {
        ProviderManager(
            providerDao = database.providerDao(),
            secretStorage = secretStorage,
            settingsRepository = settingsRepository
        )
    }

    val documentRepository by lazy {
        DocumentRepository(
            context = this,
            documentDao = database.documentDao(),
            pageTextDao = database.pageTextDao(),
            pdfExtractor = pdfTextExtractor,
            secretStorage = secretStorage,
            settingsRepository = settingsRepository
        )
    }

    val lessonRepository by lazy {
        LessonRepository(
            lessonDao = database.lessonDao(),
            documentDao = database.documentDao(),
            pageTextDao = database.pageTextDao(),
            quizDao = database.quizDao(),
            providerManager = providerManager
        )
    }

    val narrationManager by lazy { NarrationManager(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            providerManager.ensureDefaultProvidersSeeded()
        }
    }
}
