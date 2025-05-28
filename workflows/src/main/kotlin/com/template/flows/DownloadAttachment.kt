package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import java.io.File
import java.io.InputStream

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class DownloadAttachment(
    private val attachmentId: String,
    private val path: String,
) : FlowLogic<String>() {
    companion object {
        val DOWNLOAD_ATTACHMENT = ProgressTracker.Step("Download attachment")

        fun tracker() =
            ProgressTracker(
                DOWNLOAD_ATTACHMENT,
            )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): String {
        progressTracker.currentStep = DOWNLOAD_ATTACHMENT
        val content = serviceHub.attachments.openAttachment(SecureHash.parse(attachmentId))!!
        content.open().toFile(path)

        return "Downloaded file:$attachmentId to $path"
    }
}

fun InputStream.toFile(path: String) {
    File(path).outputStream().use { this.copyTo(it) }
}
