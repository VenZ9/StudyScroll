package app.studyscroll.core

object Constants {
    /**
     * Strict 25 MiB limit in bytes: 25 * 1024 * 1024 = 26,214,400 bytes.
     */
    const val MAX_PDF_BYTES: Long = 25L * 1024L * 1024L // 26,214,400 bytes

    const val ERROR_PDF_EXCEEDS_LIMIT = "PDF exceeds the 25 MB limit. Choose a smaller file."
    const val ERROR_PDF_EMPTY = "The selected file is empty (0 bytes)."
    const val ERROR_PDF_UNREADABLE = "Unable to read the selected file."
    const val ERROR_PDF_CORRUPT = "The selected file is corrupt or not a valid PDF."
    const val ERROR_PDF_PASSWORD_PROTECTED = "Password-protected PDFs are not currently supported. Please provide an unencrypted PDF."
    const val ERROR_PDF_NO_TEXT = "No extractable text found in PDF. Scanned or image-only PDFs require OCR, which is not available offline."

    const val ERROR_LOCAL_ONLY_ACTIVE = "Local-only mode is active. Cloud requests are blocked."
    const val ERROR_CLOUD_CONSENT_REQUIRED = "Cloud processing consent has not been granted. Please enable consent in Settings before using AI providers."

    const val KILO_GATEWAY_DEFAULT_URL = "https://api.kilo.ai/v1"
    const val GROQ_DEFAULT_URL = "https://api.groq.com/openai/v1"
    const val GEMINI_DEFAULT_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val NVIDIA_DEFAULT_URL = "https://integrate.api.nvidia.com/v1"
}
