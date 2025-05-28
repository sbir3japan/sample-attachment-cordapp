package com.template.webserver.models

data class SendAttachmentRequest(
    val receiver: String, // X500 name of the receiver party
    val zipPath: String, // Path to the zip file to be sent
)
