package app.studyscroll.data.ai

import app.studyscroll.core.Constants
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

class GeminiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : LessonGenerationProvider {

    override val providerType: ProviderType = ProviderType.GEMINI
    override val displayName: String = "Google Gemini"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildEndpoint(baseUrl: String, modelId: String, apiKey: String?): String {
        val cleanBase = baseUrl.trimEnd('/')
        val keyParam = if (!apiKey.isNullOrBlank()) "?key=$apiKey" else ""
        return "$cleanBase/models/$modelId:generateContent$keyParam"
    }

    override suspend fun testConnection(profile: ProviderProfile, apiKey: String?): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Google Gemini requires an API key."))
        }

        val endpoint = buildEndpoint(profile.baseUrl, profile.modelId, apiKey)

        val payload = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "ping") })
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                when {
                    response.isSuccessful -> {
                        Result.success("Connection successful! Gemini model ${profile.modelId} responded.")
                    }
                    response.code == 400 && body.contains("API_KEY_INVALID") -> {
                        Result.failure(IllegalArgumentException("Invalid Google Gemini API key."))
                    }
                    response.code == 404 -> {
                        Result.failure(IllegalArgumentException("Gemini model not found (404): ${profile.modelId}"))
                    }
                    response.code == 429 -> {
                        Result.failure(IllegalArgumentException("Google Gemini quota / rate limit exceeded (429)."))
                    }
                    else -> {
                        Result.failure(IOException("Gemini returned HTTP ${response.code}: ${body.take(150)}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network error: ${e.message ?: "Failed to reach Gemini API"}"))
        }
    }

    override suspend fun generateLessons(
        profile: ProviderProfile,
        apiKey: String?,
        request: GenerationRequest
    ): Result<List<RawLesson>> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Google Gemini requires an API key."))
        }

        val endpoint = buildEndpoint(profile.baseUrl, profile.modelId, apiKey)
        val systemPrompt = AiPromptBuilder.buildSystemPrompt()
        val userPrompt = AiPromptBuilder.buildUserPrompt(request.documentTitle, request.textBlocks)

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Gemini API error (${response.code}): ${body.take(150)}"))
                }

                val jsonResponse = JSONObject(body)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(IllegalStateException("No candidates returned by Gemini."))
                }

                val candidate = candidates.getJSONObject(0)
                val contentObj = candidate.getJSONObject("content")
                val parts = contentObj.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")

                val lessons = AiResponseParser.parseJsonResponse(text, request.textBlocks)
                if (lessons.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("Gemini returned empty lessons list."))
                }

                Result.success(lessons)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = Constants.GEMINI_DEFAULT_URL
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        val POPULAR_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )
    }
}
