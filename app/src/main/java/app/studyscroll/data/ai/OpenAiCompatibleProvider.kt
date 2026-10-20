package app.studyscroll.data.ai

import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

open class OpenAiCompatibleProvider(
    override val providerType: ProviderType,
    override val displayName: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : LessonGenerationProvider {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    protected open fun buildUrl(baseUrl: String, path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = path.trimStart('/')
        // Prevent duplicate /v1
        return if (cleanBase.endsWith("/v1") && cleanPath.startsWith("v1/")) {
            "$cleanBase/${cleanPath.removePrefix("v1/")}"
        } else {
            "$cleanBase/$cleanPath"
        }
    }

    protected open fun addHeaders(builder: Request.Builder, profile: ProviderProfile, apiKey: String?) {
        builder.addHeader("Content-Type", "application/json")
        if (!apiKey.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }
    }

    override suspend fun testConnection(profile: ProviderProfile, apiKey: String?): Result<String> = withContext(Dispatchers.IO) {
        val url = buildUrl(profile.baseUrl, "chat/completions")

        val payload = JSONObject().apply {
            put("model", profile.modelId)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                })
            }
            put("messages", messages)
            put("max_tokens", 5)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))

        addHeaders(requestBuilder, profile, apiKey)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                when {
                    response.isSuccessful -> {
                        Result.success("Connection successful! Model ${profile.modelId} responded.")
                    }
                    response.code == 401 -> {
                        Result.failure(IllegalArgumentException("Authentication failed (401). Please check your API key."))
                    }
                    response.code == 403 -> {
                        Result.failure(IllegalArgumentException("Access forbidden (403). Your key or model selection lacks permission."))
                    }
                    response.code == 404 -> {
                        Result.failure(IllegalArgumentException("Endpoint or model not found (404). Check Base URL and Model ID."))
                    }
                    response.code == 429 -> {
                        Result.failure(IllegalArgumentException("Rate limit reached (429). Please try again later."))
                    }
                    response.code >= 500 -> {
                        Result.failure(IOException("Server error (${response.code}). The provider is currently unavailable."))
                    }
                    else -> {
                        Result.failure(IOException("Provider returned HTTP ${response.code}: ${body.take(150)}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network error: ${e.message ?: "Failed to connect to provider"}"))
        }
    }

    override suspend fun generateLessons(
        profile: ProviderProfile,
        apiKey: String?,
        request: GenerationRequest
    ): Result<List<RawLesson>> = withContext(Dispatchers.IO) {
        val url = buildUrl(profile.baseUrl, "chat/completions")

        val systemPrompt = AiPromptBuilder.buildSystemPrompt()
        val userPrompt = AiPromptBuilder.buildUserPrompt(request.documentTitle, request.textBlocks)

        val payload = JSONObject().apply {
            put("model", profile.modelId)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messages)
            put("temperature", 0.2)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))

        addHeaders(requestBuilder, profile, apiKey)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "Authentication error (401). Please re-enter your API key."
                        429 -> "Rate limit exceeded (429). Please wait before trying again."
                        else -> "Provider failed with HTTP ${response.code}: ${body.take(150)}"
                    }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val jsonResponse = JSONObject(body)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.failure(IllegalStateException("No completion choices returned by provider."))
                }

                val content = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val parsedLessons = AiResponseParser.parseJsonResponse(content, request.textBlocks)
                if (parsedLessons.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("Provider returned empty or unparseable lesson list."))
                }

                Result.success(parsedLessons)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
