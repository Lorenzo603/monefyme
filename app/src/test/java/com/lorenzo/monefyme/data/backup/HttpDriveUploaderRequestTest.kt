package com.lorenzo.monefyme.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the Drive API v3 multipart upload request construction (metadata part, MIME part,
 * raw CSV bytes part) without any network call.
 */
class HttpDriveUploaderRequestTest {
    private val csvBytes = "header\nrow\n".toByteArray(Charsets.UTF_8)
    private val request = DriveUploadRequest(
        fileName = "Monefy.Backup.2026-02-03.csv",
        mimeType = "text/csv",
        content = csvBytes,
    )

    @Test
    fun uploadTargetsTheDriveMultipartEndpoint() {
        assertEquals(
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
            HttpDriveUploader.UPLOAD_ENDPOINT,
        )
    }

    @Test
    fun metadataNamesTheFileAndDeclaresTheCsvMimeType() {
        assertEquals(
            "{\"name\":\"Monefy.Backup.2026-02-03.csv\",\"mimeType\":\"text/csv\"}",
            HttpDriveUploader.buildMetadataJson(request),
        )
    }

    @Test
    fun metadataEscapesQuotesAndBackslashesInTheFileName() {
        val tricky = DriveUploadRequest("we\"ird\\name.csv", "text/csv", csvBytes)

        assertEquals(
            "{\"name\":\"we\\\"ird\\\\name.csv\",\"mimeType\":\"text/csv\"}",
            HttpDriveUploader.buildMetadataJson(tricky),
        )
    }

    @Test
    fun multipartBodyCarriesMetadataThenRawCsvBytesBetweenBoundaryMarkers() {
        val boundary = "monefy_test_boundary"

        val body = HttpDriveUploader.buildMultipartBody(request, boundary).toString(Charsets.UTF_8)

        assertTrue(body.startsWith("--monefy_test_boundary\r\n"))
        assertTrue(body.contains("Content-Type: application/json; charset=UTF-8\r\n"))
        assertTrue(body.contains("{\"name\":\"Monefy.Backup.2026-02-03.csv\",\"mimeType\":\"text/csv\"}"))
        assertTrue(body.contains("Content-Type: text/csv\r\n"))
        assertTrue(
            "The CSV bytes must appear unchanged between the metadata and the closing boundary.",
            body.contains("header\nrow\n"),
        )
        assertTrue(body.endsWith("\r\n--monefy_test_boundary--\r\n"))
    }

    @Test
    fun multipartBodyKeepsTheExactContentByteCount() {
        val boundary = "b"

        val body = HttpDriveUploader.buildMultipartBody(request, boundary)
        val headerEnd = "Content-Type: text/csv\r\n\r\n".toByteArray(Charsets.UTF_8)
        val headerOffset = findIndexOf(body, headerEnd)
        val trailer = "\r\n--b--\r\n".toByteArray(Charsets.UTF_8)

        assertTrue(headerOffset >= 0)
        val contentRegion = body.copyOfRange(headerOffset + headerEnd.size, body.size - trailer.size)
        assertTrue(contentRegion.contentEquals(csvBytes))
    }

    private fun findIndexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (start in 0..haystack.size - needle.size) {
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) continue@outer
            }
            return start
        }
        return -1
    }
}
