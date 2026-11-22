package com.lorenzo.monefyme.data.backup

/**
 * A single Google Drive file upload: the file name shown in Drive, the MIME type, and the
 * exact bytes to store. The content is the byte-identical output of the CSV export.
 */
data class DriveUploadRequest(
    val fileName: String,
    val mimeType: String,
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DriveUploadRequest) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}

/**
 * Uploads a [DriveUploadRequest] with the given OAuth access token. Implementations throw
 * [java.io.IOException] when the upload fails; unit tests drive a fake instead of the real
 * Drive API.
 */
interface DriveUploader {
    suspend fun upload(accessToken: String, request: DriveUploadRequest)
}
