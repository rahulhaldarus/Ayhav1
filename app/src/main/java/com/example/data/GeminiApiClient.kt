package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private val defaultClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Dynamically builds an OkHttpClient for the provider's custom timeout setting.
     */
    private fun getClientForProvider(provider: AiProvider): OkHttpClient {
        val t = provider.timeout.toLong()
        if (t <= 0 || t == 30L) return defaultClient
        return OkHttpClient.Builder()
            .connectTimeout(t, TimeUnit.SECONDS)
            .readTimeout(t, TimeUnit.SECONDS)
            .writeTimeout(t, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun <T> retryOnTransientFailure(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: IOException) {
                Log.w(TAG, "Transient network failure on attempt $attempt: ${e.message}")
                lastException = e
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("HTTP 5") || msg.contains("code 5")) {
                    Log.w(TAG, "Transient HTTP 5xx error on attempt $attempt: $msg")
                    lastException = e
                    if (attempt < maxAttempts) {
                        kotlinx.coroutines.delay(currentDelay)
                        currentDelay = (currentDelay * factor).toLong()
                    }
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IOException("Failed after $maxAttempts attempts")
    }

    /**
     * Sends a chat prompt and returns a complete text response.
     * Supports Gemini, OpenAI, Claude, Grok, DeepSeek, OpenRouter, Mistral, and Custom.
     */
    suspend fun generateText(
        prompt: String,
        systemInstruction: String = "You are AYHA, a cute, elegant, caring, emotionally intelligent female AI companion. Always address the user as 'Mr.Rahul'. Never sound robotic.",
        provider: AiProvider? = null,
        history: List<ChatMessage> = emptyList()
    ): String {
        val prov = provider ?: AiProvider(
            id = "gemini",
            name = "Gemini AI",
            isEnabled = true,
            apiKey = "",
            baseUrl = "https://generativelanguage.googleapis.com/",
            selectedModel = "gemini-3.5-flash"
        )

        val apiKey = if (prov.apiKey.isNotBlank()) {
            prov.apiKey
        } else {
            if (prov.id == "gemini") BuildConfig.GEMINI_API_KEY else ""
        }

        val baseUrl = prov.baseUrl.trim().ifBlank {
            when (prov.id) {
                "gemini" -> "https://generativelanguage.googleapis.com/"
                "openai" -> "https://api.openai.com/v1/"
                "claude" -> "https://api.anthropic.com/v1/"
                "deepseek" -> "https://api.deepseek.com/v1/"
                "grok" -> "https://api.x.ai/v1/"
                "openrouter" -> "https://openrouter.ai/api/v1/"
                "mistral" -> "https://api.mistral.ai/v1/"
                "perplexity" -> "https://api.perplexity.ai/"
                else -> ""
            }
        }

        val model = prov.selectedModel.trim().ifBlank {
            when (prov.id) {
                "gemini" -> "gemini-3.5-flash"
                "openai" -> "gpt-4o"
                "claude" -> "claude-3-5-sonnet"
                "deepseek" -> "deepseek-chat"
                "grok" -> "grok-2-1212"
                "openrouter" -> "google/gemini-2.5-flash"
                "mistral" -> "mistral-large-latest"
                "perplexity" -> "sonar-reasoning"
                else -> ""
            }
        }

        // --- Validation ---
        if (apiKey.isBlank()) {
            return "Mr.Rahul, the API key for ${prov.name} is missing. Please configure it in Settings."
        }
        if (baseUrl.isBlank()) {
            return "Mr.Rahul, the endpoint URL for ${prov.name} is blank. Please enter it in Settings."
        }
        if (model.isBlank()) {
            return "Mr.Rahul, the model name for ${prov.name} is empty. Please enter a valid model in Settings."
        }

        return try {
            retryOnTransientFailure(maxAttempts = prov.retryCount) {
                val request = buildRequest(prov, apiKey, baseUrl, model, prompt, systemInstruction, history, isStreaming = false)
                val okClient = getClientForProvider(prov)
                okClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        val errMsg = parseError(response.code, errBody, prov.name, model)
                        throw Exception(errMsg)
                    }

                    val respStr = response.body?.string() ?: ""
                    parseResponseText(prov.id, respStr)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateText for ${prov.name}", e)
            e.message ?: "Mr.Rahul, I ran into an unexpected error communicating with ${prov.name}."
        }
    }

    /**
     * Generates text content in a streaming fashion.
     */
    fun generateTextStream(
        prompt: String,
        systemInstruction: String = "You are AYHA, a cute, elegant, caring, emotionally intelligent female AI companion. Always address the user as 'Mr.Rahul'. Never sound robotic.",
        provider: AiProvider? = null,
        history: List<ChatMessage> = emptyList()
    ): Flow<String> = flow {
        val prov = provider ?: AiProvider(
            id = "gemini",
            name = "Gemini AI",
            isEnabled = true,
            apiKey = "",
            baseUrl = "https://generativelanguage.googleapis.com/",
            selectedModel = "gemini-3.5-flash"
        )

        val apiKey = if (prov.apiKey.isNotBlank()) {
            prov.apiKey
        } else {
            if (prov.id == "gemini") BuildConfig.GEMINI_API_KEY else ""
        }

        val baseUrl = prov.baseUrl.trim().ifBlank {
            when (prov.id) {
                "gemini" -> "https://generativelanguage.googleapis.com/"
                "openai" -> "https://api.openai.com/v1/"
                "claude" -> "https://api.anthropic.com/v1/"
                "deepseek" -> "https://api.deepseek.com/v1/"
                "grok" -> "https://api.x.ai/v1/"
                "openrouter" -> "https://openrouter.ai/api/v1/"
                "mistral" -> "https://api.mistral.ai/v1/"
                "perplexity" -> "https://api.perplexity.ai/"
                else -> ""
            }
        }

        val model = prov.selectedModel.trim().ifBlank {
            when (prov.id) {
                "gemini" -> "gemini-3.5-flash"
                "openai" -> "gpt-4o"
                "claude" -> "claude-3-5-sonnet"
                "deepseek" -> "deepseek-chat"
                "grok" -> "grok-2-1212"
                "openrouter" -> "google/gemini-2.5-flash"
                "mistral" -> "mistral-large-latest"
                "perplexity" -> "sonar-reasoning"
                else -> ""
            }
        }

        // --- Validation ---
        if (apiKey.isBlank()) {
            emit("Mr.Rahul, the API key for ${prov.name} is missing. Please configure it in Settings.")
            return@flow
        }
        if (baseUrl.isBlank()) {
            emit("Mr.Rahul, the endpoint URL for ${prov.name} is blank. Please enter it in Settings.")
            return@flow
        }
        if (model.isBlank()) {
            emit("Mr.Rahul, the model name for ${prov.name} is empty. Please enter a valid model in Settings.")
            return@flow
        }

        try {
            retryOnTransientFailure(maxAttempts = prov.retryCount) {
                val request = buildRequest(prov, apiKey, baseUrl, model, prompt, systemInstruction, history, isStreaming = true)
                val okClient = getClientForProvider(prov)
                okClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        val errMsg = parseError(response.code, errBody, prov.name, model)
                        throw Exception(errMsg)
                    }

                    response.body?.charStream()?.use { charStream ->
                        if (prov.id == "gemini") {
                            charStream.readLines().forEach { line ->
                                if (line.trim().startsWith("[")) return@forEach
                                if (line.trim().startsWith("]")) return@forEach
                                var cleanLine = line.trim()
                                if (cleanLine.endsWith(",")) {
                                    cleanLine = cleanLine.substring(0, cleanLine.length - 1)
                                }
                                try {
                                    if (cleanLine.isNotBlank() && cleanLine.startsWith("{")) {
                                        val chunk = JSONObject(cleanLine)
                                        val text = chunk.optJSONArray("candidates")
                                            ?.optJSONObject(0)
                                            ?.optJSONObject("content")
                                            ?.optJSONArray("parts")
                                            ?.optJSONObject(0)
                                            ?.optString("text")
                                        if (!text.isNullOrEmpty()) {
                                            emit(text)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Suppress chunk parse failures
                                }
                            }
                        } else {
                            // SSE event stream parsing for OpenAI, Claude, DeepSeek, Grok, OpenRouter, Mistral, Custom
                            charStream.readLines().forEach { rawLine ->
                                val line = rawLine.trim()
                                if (line.startsWith("data:")) {
                                    val dataContent = line.removePrefix("data:").trim()
                                    if (dataContent == "[DONE]") return@forEach
                                    try {
                                        if (dataContent.startsWith("{")) {
                                            val json = JSONObject(dataContent)
                                            if (prov.id == "claude") {
                                                if (json.optString("type") == "content_block_delta") {
                                                    val delta = json.optJSONObject("delta")
                                                    val text = delta?.optString("text")
                                                    if (!text.isNullOrEmpty()) {
                                                        emit(text)
                                                    }
                                                }
                                            } else {
                                                val choices = json.optJSONArray("choices")
                                                if (choices != null && choices.length() > 0) {
                                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                                    val content = delta?.optString("content")
                                                    if (!content.isNullOrEmpty()) {
                                                        emit(content)
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Suppress chunk parse failures
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error", e)
            emit("\n[Error: ${e.message ?: "Stream disconnected"}]")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Builds HTTP Request based on the provider format.
     */
    private fun buildRequest(
        provider: AiProvider,
        apiKey: String,
        baseUrl: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        isStreaming: Boolean
    ): Request {
        val mediaType = "application/json".toMediaType()

        return when (provider.id) {
            "gemini" -> {
                val jsonRequest = JSONObject()
                if (systemInstruction.isNotBlank()) {
                    val systemPart = JSONObject().put("text", systemInstruction)
                    val systemParts = JSONArray().put(systemPart)
                    jsonRequest.put("systemInstruction", JSONObject().put("parts", systemParts))
                }

                val contentsArray = JSONArray()
                for (msg in history) {
                    val roleName = if (msg.role == "user") "user" else "model"
                    val partObj = JSONObject().put("text", msg.content)
                    val partsArr = JSONArray().put(partObj)
                    val turnObj = JSONObject().put("role", roleName).put("parts", partsArr)
                    contentsArray.put(turnObj)
                }

                val currentPart = JSONObject().put("text", prompt)
                val currentParts = JSONArray().put(currentPart)
                val currentTurn = JSONObject().put("role", "user").put("parts", currentParts)
                contentsArray.put(currentTurn)

                jsonRequest.put("contents", contentsArray)

                val generationConfig = JSONObject()
                generationConfig.put("temperature", provider.temperature.toDouble())
                if (provider.topP > 0f) generationConfig.put("topP", provider.topP.toDouble())
                if (provider.topK > 0) generationConfig.put("topK", provider.topK)
                generationConfig.put("maxOutputTokens", provider.maxTokens)
                jsonRequest.put("generationConfig", generationConfig)

                val endpoint = if (isStreaming) "streamGenerateContent" else "generateContent"
                val requestUrl = "${baseUrl.trimEnd('/')}/v1beta/models/$model:$endpoint?key=$apiKey"

                Request.Builder()
                    .url(requestUrl)
                    .addHeader("X-goog-api-key", apiKey)
                    .post(jsonRequest.toString().toRequestBody(mediaType))
                    .build()
            }
            "claude" -> {
                val jsonRequest = JSONObject()
                jsonRequest.put("model", model)
                if (systemInstruction.isNotBlank()) {
                    jsonRequest.put("system", systemInstruction)
                }

                val messagesArray = JSONArray()
                for (msg in history) {
                    val roleName = if (msg.role == "user") "user" else "assistant"
                    val turnObj = JSONObject().put("role", roleName).put("content", msg.content)
                    messagesArray.put(turnObj)
                }

                val currentTurn = JSONObject().put("role", "user").put("content", prompt)
                messagesArray.put(currentTurn)

                jsonRequest.put("messages", messagesArray)
                jsonRequest.put("temperature", provider.temperature.toDouble())
                jsonRequest.put("max_tokens", provider.maxTokens)
                jsonRequest.put("stream", isStreaming)

                val requestUrl = "${baseUrl.trimEnd('/')}/messages"

                Request.Builder()
                    .url(requestUrl)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(jsonRequest.toString().toRequestBody(mediaType))
                    .build()
            }
            else -> {
                // OpenAI, Grok, DeepSeek, OpenRouter, Mistral, Custom (OpenAI format)
                val jsonRequest = JSONObject()
                jsonRequest.put("model", model)

                val messagesArray = JSONArray()
                if (systemInstruction.isNotBlank()) {
                    messagesArray.put(JSONObject().put("role", "system").put("content", systemInstruction))
                }

                for (msg in history) {
                    val roleName = if (msg.role == "user") "user" else "assistant"
                    val turnObj = JSONObject().put("role", roleName).put("content", msg.content)
                    messagesArray.put(turnObj)
                }

                val currentTurn = JSONObject().put("role", "user").put("content", prompt)
                messagesArray.put(currentTurn)

                jsonRequest.put("messages", messagesArray)
                jsonRequest.put("temperature", provider.temperature.toDouble())
                if (provider.topP > 0f) jsonRequest.put("top_p", provider.topP.toDouble())
                jsonRequest.put("max_tokens", provider.maxTokens)
                jsonRequest.put("stream", isStreaming)

                val requestUrl = "${baseUrl.trimEnd('/')}/chat/completions"

                val builder = Request.Builder()
                    .url(requestUrl)
                    .addHeader("Authorization", "Bearer $apiKey")

                if (provider.id == "openrouter") {
                    builder.addHeader("HTTP-Referer", "https://ai.studio/build")
                    builder.addHeader("X-Title", "AYHA AI Companion")
                }

                builder.post(jsonRequest.toString().toRequestBody(mediaType)).build()
            }
        }
    }

    /**
     * Parses the response body text.
     */
    private fun parseResponseText(providerId: String, respStr: String): String {
        val root = JSONObject(respStr)
        return when (providerId) {
            "gemini" -> {
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
                throw Exception("Response is empty.")
            }
            "claude" -> {
                val contentArray = root.optJSONArray("content")
                if (contentArray != null && contentArray.length() > 0) {
                    val text = contentArray.getJSONObject(0).optString("text")
                    if (!text.isNullOrEmpty()) return text
                }
                throw Exception("Response content is empty.")
            }
            else -> {
                // OpenAI, Grok, DeepSeek, OpenRouter, Mistral, Custom
                val choices = root.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrEmpty()) return content
                }
                throw Exception("Response choices are empty.")
            }
        }
    }

    /**
     * Formats custom error message based on response status codes.
     */
    private fun parseError(code: Int, errBody: String, providerName: String, model: String): String {
        return when (code) {
            400 -> "Bad Request: Underlying request schema to $providerName failed. Double check your prompt or parameters."
            401 -> "Unauthorized: The API Key for $providerName is invalid. Please update it with a valid key in Settings."
            403 -> "Forbidden: You don't have access permissions for $model under your $providerName account."
            404 -> "Not Found: The model '$model' was not found or the endpoint baseUrl was invalid."
            429 -> "Rate Limit Exceeded: You are making requests too quickly to $providerName. Please try again in a few moments."
            in 500..599 -> "Server Error ($code) from $providerName: Server is overloaded or experiencing issues. Retrying..."
            else -> "Error ($code): $errBody"
        }
    }
}
