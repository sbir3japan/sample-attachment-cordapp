package com.template.webserver

import com.template.flows.DownloadAttachment
import com.template.flows.SendAttachment
import com.template.webserver.models.DownloadAttachmentRequest
import com.template.webserver.models.SendAttachmentRequest
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class AttachmentController(
    rpc: NodeRPCConnection,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @PostMapping(
        value = ["/sendAttachment"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @ResponseBody
    private fun sendAttachment(
        @RequestBody input: SendAttachmentRequest,
    ): ResponseEntity<Any> {
        try {
            val receiver: Party =
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(input.receiver))
                    ?: throw IllegalArgumentException("${input.receiver} not found in network map.")

            val tx: SignedTransaction =
                proxy
                    .startTrackedFlowDynamic(
                        SendAttachment::class.java,
                        receiver,
                        input.zipPath,
                    ).returnValue
                    .getOrThrow()

            val result =
                object {
                    val txHash = tx.id.toString()
                    val attachmentId =
                        // First attachment is the contract jar, last attachment is the target zip file.
                        tx.tx.attachments
                            .last()
                            .toString()
                }

            return ResponseEntity.ok(result)
        } catch (e: Exception) {
            return ResponseEntity.ok(
                object {
                    val error = e.toString()
                },
            )
        }
    }

    @GetMapping(value = ["/downloadAttachment"], produces = ["application/json"])
    @ResponseBody
    private fun downloadAttachment(
        @RequestBody input: DownloadAttachmentRequest,
    ): ResponseEntity<Any> =
        try {
            val result: String =
                proxy
                    .startTrackedFlowDynamic(
                        DownloadAttachment::class.java,
                        input.attachmentId,
                        input.path,
                    ).returnValue
                    .get() ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Requested file: ${input.path} is not found.",
                )

            ResponseEntity.ok(
                object {
                    val result = result
                },
            )
        } catch (e: Exception) {
            ResponseEntity.ok(
                object {
                    val error = e.toString()
                },
            )
        }
}
