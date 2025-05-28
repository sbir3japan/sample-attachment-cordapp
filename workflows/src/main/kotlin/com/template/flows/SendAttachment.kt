package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.InvoiceContract
import com.template.states.InvoiceState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.io.File

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SendAttachment(
    private val receiver: Party,
    private val zipPath: String,
) : FlowLogic<SignedTransaction>() {
    companion object {
        val GENERATING_TRANSACTION = ProgressTracker.Step("Generating transaction")

        val PROCESS_TRANSACTION = ProgressTracker.Step("PROCESS transaction")

        val FINALISING_TRANSACTION =
            ProgressTracker.Step("Obtaining notary signature and recording transaction.")

        fun tracker() =
            ProgressTracker(
                GENERATING_TRANSACTION,
                PROCESS_TRANSACTION,
                FINALISING_TRANSACTION,
            )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary =
            serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

        val transactionBuilder = TransactionBuilder(notary)

        val path = System.getProperty("user.dir")
        println("Working Directory = $path")

        val attachmentHash =
            SecureHash.parse(
                uploadAttachment(
                    zipPath,
                    serviceHub,
                    ourIdentity,
                    null,
                ),
            )

        progressTracker.currentStep = GENERATING_TRANSACTION
        val output =
            InvoiceState(attachmentHash.toString(), participants = listOf(ourIdentity, receiver))
        val commandData = InvoiceContract.Commands.Issue()
        transactionBuilder.addCommand(commandData, ourIdentity.owningKey, receiver.owningKey)
        transactionBuilder.addOutputState(output, InvoiceContract.ID)
        transactionBuilder.addAttachment(attachmentHash)
        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = PROCESS_TRANSACTION
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        progressTracker.currentStep = FINALISING_TRANSACTION

        val session = initiateFlow(receiver)
        val fullySignedTransaction =
            subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))
    }
}

private fun uploadAttachment(
    path: String,
    service: ServiceHub,
    whoAmI: Party,
    fileName: String?,
): String {
    val attachmentHash =
        service.attachments.importAttachment(
            File(path).inputStream(),
            whoAmI.toString(),
            fileName,
        )

    return attachmentHash.toString()
}

@InitiatedBy(SendAttachment::class)
class SendAttachmentResponder(
    val counterpartySession: FlowSession,
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow =
            object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) =
                    requireThat {
                        if (stx.tx.attachments.isEmpty()) {
                            throw FlowException("No Jar was being sent")
                        }
                    }
            }
        val txId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}
