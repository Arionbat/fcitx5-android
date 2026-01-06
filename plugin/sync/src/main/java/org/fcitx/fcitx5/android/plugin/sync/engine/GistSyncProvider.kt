package org.fcitx.fcitx5.android.plugin.sync.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.io.IOException

class GistSyncProvider(
    private val client: OkHttpClient,
    private val token: String,
    private val gistId: String? = null // If null, create new on upload?
) : SyncProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun connect(): Result<Boolean> {
        // Check user info
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "token $token")
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Failed to connect to GitHub: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun list(path: String): Result<List<String>> {
         // List files in the gist
         if (gistId.isNullOrEmpty()) return Result.success(emptyList())

         val request = Request.Builder()
            .url("https://api.github.com/gists/$gistId")
            .header("Authorization", "token $token")
            .get()
            .build()
        
         return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return Result.failure(IOException("Empty body"))
                    val jsonElement = json.parseToJsonElement(body)
                    val files = jsonElement.jsonObject["files"]?.jsonObject?.keys?.toList() ?: emptyList()
                    Result.success(files)
                } else {
                    Result.failure(IOException("Failed to list gist files"))
                }
            }
        } catch (e: Exception) {
             Result.failure(e)
        }
    }

    override suspend fun upload(path: String, content: InputStream): Result<Boolean> {
        // Note: Gist API expects all files in one JSON body for update/create.
        // Single file upload is actually a PATCH to the gist.
        if (gistId.isNullOrEmpty()) return Result.failure(IOException("Gist ID required for single file upload"))

        val contentString = content.reader().readText() // Warning: Memory intensive for large files
        
        // Structure: { "files": { "filename": { "content": "..." } } }
        val payload = """
            {
                "files": {
                    "$path": {
                        "content": ${Json.encodeToString(contentString)}
                    }
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.github.com/gists/$gistId")
            .header("Authorization", "token $token")
            .header("Accept", "application/vnd.github.v3+json")
            .patch(payload.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Failed to upload to Gist: ${response.code}"))
                }
            }
        } catch (e: Exception) {
             Result.failure(e)
        }
    }

    override suspend fun download(path: String): Result<InputStream> {
         if (gistId.isNullOrEmpty()) return Result.failure(IOException("Gist ID required"))

         val request = Request.Builder()
            .url("https://api.github.com/gists/$gistId")
            .header("Authorization", "token $token")
            .get()
            .build()
        
         return try {
            val response = client.newCall(request).execute() // Don't use block here to return stream
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty body")
                val jsonElement = json.parseToJsonElement(body)
                val fileObj = jsonElement.jsonObject["files"]?.jsonObject?.get(path)?.jsonObject
                
                // For proper download, usually get "raw_url" and fetch that, or use "content" field
                val content = fileObj?.get("content")?.jsonPrimitive?.contentOrNull
                
                if (content != null) {
                    Result.success(content.byteInputStream())
                } else {
                    // Try raw_url?
                    val rawUrl = fileObj?.get("raw_url")?.jsonPrimitive?.contentOrNull
                    if (rawUrl != null) {
                         // Need another request
                         val rawRequest = Request.Builder().url(rawUrl).get().build()
                         val rawResponse = client.newCall(rawRequest).execute()
                         if (rawResponse.isSuccessful) {
                             Result.success(rawResponse.body!!.byteStream())
                         } else {
                             Result.failure(IOException("Failed to fetch raw content"))
                         }
                    } else {
                        Result.failure(IOException("File not found in Gist"))
                    }
                }
            } else {
                response.close()
                Result.failure(IOException("Failed to get Gist info"))
            }
        } catch (e: Exception) {
             Result.failure(e)
        }
    }
}
