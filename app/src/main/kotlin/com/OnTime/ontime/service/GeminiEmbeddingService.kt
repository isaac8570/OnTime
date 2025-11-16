package com.OnTime.ontime.service

import com.OnTime.ontime.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiEmbeddingService {
    
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.GEMINI_API_KEY
    
    suspend fun getEmbedding(text: String): List<Double>? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=$apiKey"
            
            val requestBody = JSONObject().apply {
                put("model", "models/text-embedding-004")
                put("content", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    })
                })
            }
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                val embedding = jsonResponse.getJSONObject("embedding")
                val values = embedding.getJSONArray("values")
                
                val result = mutableListOf<Double>()
                for (i in 0 until values.length()) {
                    result.add(values.getDouble(i))
                }
                result
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // 코사인 유사도 계산
    fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        if (a.size != b.size) return 0.0
        
        val dotProduct = a.zip(b) { x, y -> x * y }.sum()
        val normA = kotlin.math.sqrt(a.sumOf { it * it })
        val normB = kotlin.math.sqrt(b.sumOf { it * it })
        
        return if (normA == 0.0 || normB == 0.0) 0.0 else dotProduct / (normA * normB)
    }
}
