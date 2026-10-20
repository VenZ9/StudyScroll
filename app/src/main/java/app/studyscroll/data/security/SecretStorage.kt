package app.studyscroll.data.security

interface SecretStorage {
    fun saveApiKey(providerId: String, apiKey: String)
    fun getApiKey(providerId: String): String?
    fun hasApiKey(providerId: String): Boolean
    fun removeApiKey(providerId: String)
    fun clearAll()
}
