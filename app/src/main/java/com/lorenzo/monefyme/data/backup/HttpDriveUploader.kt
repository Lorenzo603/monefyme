package com.lorenzo.monefyme.data.backup

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [DriveUploader] that performs a Drive API v3 multipart upload
 * (`POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`) with the
 * OAuth bearer token. The file lands in the root of the user's My Drive (no parents field).
 *
 * Fails with [IOException] on transport errors or any non-2xx Drive response.
 */
class HttpDriveUploader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DriveUploader {

    override suspend fun upload(accessToken: String, request: DriveUploadRequest) {
        withContext(ioDispatcher) {
            val boundary = "monefy_" + UUID.randomUUID().toString().replace("-", "")
            val body = buildMultipartBody(request, boundary)
            val connection = URL(UPLOAD_ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "multipart/mixed; boundary=\"$boundary\"")
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { output -> output.write(body) }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val detail = connection.errorStream?.use { error ->
                        error.readBytes().toString(Charsets.UTF_8)
                    }?.take(MAX_ERROR_DETAIL_CHARS).orEmpty()
                    val suffix = if (detail.isBlank()) "" else " ($detail)"
                    throw IOException("Google Drive rejected the upload with HTTP $responseCode$suffix.")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        internal const val UPLOAD_ENDPOINT =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        private const val TIMEOUT_MILLIS = 30_000
        private const val MAX_ERROR_DETAIL_CHARS = 200
        private const val CRLF = "\r\n"

        /** The JSON metadata part naming the file and declaring its MIME type. */
        internal fun buildMetadataJson(request: DriveUploadRequest): String {
            return "{\"name\":\"${jsonEscape(request.fileName)}\",\"mimeType\":\"${jsonEscape(request.mimeType)}\"}"
        }

        /** Builds the multipart/mixed body: metadata part followed by the raw CSV bytes part. */
        internal fun buildMultipartBody(request: DriveUploadRequest, boundary: String): ByteArray {
            val header = buildString {
                append("--").append(boundary).append(CRLF)
                append("Content-Type: application/json; charset=UTF-8").append(CRLF)
                .append(CRLF)
                append(buildMetadataJson(request)).append(CRLF)
                append("--").append(boundary).append(CRLF)
                append("Content-Type: ").append(request.mimeType).append(CRLF)
                .append(CRLF)
            }.toByteArray(Charsets.UTF_8)
            val trailer = buildString {
                append(CRLF)
                append("--").append(boundary).append("--").append(CRLF)
            }.toByteArray(Charsets.UTF_8)
            return header + request.content + trailer
        }

        private fun jsonEscape(value: String): String {
            val builder = StringBuilder()
            value.forEach { char ->
                when {
                    char == '"' -> builder.append("\\\"")
                    char == '\\' -> builder.append("\\\\")
                    char == '\n' -> builder.append("\\n")
                    char == '\r' -> builder.append("\\r")
                    char == '\t' -> builder.append("\\t")
                    else -> builder.append(char)
                }
            }
            return builder.toString()
        }
    }
}
