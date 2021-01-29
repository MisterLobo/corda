package com.r3.resolution.workflows

import com.r3.resolution.contracts.DummyState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.internal.*
import org.junit.After
import org.junit.Before

abstract class AbstractFlowTest {
    lateinit var mockNet: InternalMockNetwork

    lateinit var notaryNode: TestStartedNode
    lateinit var issuerNode: TestStartedNode
    lateinit var aliceNode: TestStartedNode
    lateinit var bobNode: TestStartedNode

    lateinit var notaryParty: Party
    lateinit var issuerParty: Party
    lateinit var aliceParty: Party
    lateinit var bobParty: Party

    lateinit var issuerLegalName: CordaX500Name
    lateinit var aliceLegalName: CordaX500Name
    lateinit var bobLegalName: CordaX500Name

    lateinit var allNotaries: List<TestStartedNode>

    @Before
    fun setup() {
        mockNet = InternalMockNetwork(
            cordappsForAllNodes = listOf(
                findCordapp("com.r3.resolution")
            ),
            notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, false)),
            initialNetworkParameters = testNetworkParameters(
                minimumPlatformVersion = 6 // 4.4
            )
        )

        allNotaries = mockNet.notaryNodes
        notaryNode = mockNet.notaryNodes.first()
        notaryParty = notaryNode.info.singleIdentity()

        issuerLegalName = CordaX500Name(organisation = "ISSUER", locality = "London", country = "GB")
        issuerNode = mockNet.createNode(InternalMockNodeParameters(legalName = issuerLegalName))
        issuerParty = issuerNode.info.singleIdentity()

        aliceLegalName = CordaX500Name(organisation = "ALICE", locality = "London", country = "GB")
        aliceNode = mockNet.createNode(InternalMockNodeParameters(legalName = aliceLegalName))
        aliceParty = aliceNode.info.singleIdentity()

        bobLegalName = CordaX500Name(organisation = "BOB", locality = "London", country = "GB")
        bobNode = mockNet.createNode(InternalMockNodeParameters(legalName = bobLegalName))
        bobParty = bobNode.info.singleIdentity()
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    fun <T> runFlow(
        node: TestStartedNode,
        flowLogic: FlowLogic<T>
    ): T {
        val flowFuture = node.services.startFlow(flowLogic).resultFuture
        mockNet.runNetwork()
        return flowFuture.getOrThrow()
    }

    fun issueDummyState(
        node: TestStartedNode,
        owner: Party
    ): StateAndRef<DummyState> {
        return runFlow(
            node,
            IssueDummyState(owner)
        )
    }

    fun moveDummyState(
        node: TestStartedNode,
        dummyState: StateAndRef<DummyState>,
        newOwner: Party
    ): StateAndRef<DummyState> {
        return runFlow(
            node,
            MoveDummyState(dummyState, newOwner)
        )
    }
}