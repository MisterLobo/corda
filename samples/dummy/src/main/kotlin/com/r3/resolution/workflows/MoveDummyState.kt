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
class MoveDummyState(
        private val dummyStateToMove: StateAndRef<DummyState>,
        private val newOwner: Party
): FlowLogic<StateAndRef<DummyState>>() {

    @Suspendable
    override fun call(): StateAndRef<DummyState> {
        val movedDummyState = DummyState(ourIdentity, newOwner)

        val txBuilder = TransactionBuilder()
        txBuilder.notary = serviceHub.networkMapCache.notaryIdentities.first()
        txBuilder.addInputState(dummyStateToMove)
        txBuilder.addOutputState(movedDummyState)
        txBuilder.addCommand(
            DummyStateContract.Commands.Move(),
            listOf(ourIdentity.owningKey)
        )

        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder)
        val ownerSession = initiateFlow(newOwner)

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

@InitiatedBy(MoveDummyState::class)
class MoveDummyStateResponder(
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