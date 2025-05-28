package com.template.webserver.models

data class DownloadAttachmentRequest(
    val attachmentId: String, // ID of the attachment to download
    val path: String, // Path where the attachment should be saved
)
