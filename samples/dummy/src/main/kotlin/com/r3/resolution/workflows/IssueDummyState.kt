package com.r3.resolution.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.resolution.contracts.DummyStateContract
import com.r3.resolution.contracts.DummyState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class IssueDummyState(
    private val owner: Party
): FlowLogic<StateAndRef<DummyState>>() {

    @Suspendable
    override fun call(): StateAndRef<DummyState> {
        val dummyState = DummyState(ourIdentity, owner)

        val txBuilder = TransactionBuilder()
        txBuilder.notary = serviceHub.networkMapCache.notaryIdentities.first()
        txBuilder.addOutputState(dummyState)
        txBuilder.addCommand(
            DummyStateContract.Commands.Issue(),
            listOf(ourIdentity.owningKey)
        )

        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder)
        val ownerSession = initiateFlow(owner)

        subFlow(
            FinalityFlow(
                transaction = locallySignedTx,
                sessions = listOf(ownerSession),
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )

        return locallySignedTx.coreTransaction.outRefsOfType<DummyState>().single()

    }
}

@InitiatedBy(IssueDummyState::class)
class IssueDummyStateResponder(
    private val otherSession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(
            ReceiveFinalityFlow(
                otherSession,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}