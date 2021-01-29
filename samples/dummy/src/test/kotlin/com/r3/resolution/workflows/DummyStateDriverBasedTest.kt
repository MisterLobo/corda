package com.r3.resolution.workflows

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import net.corda.testing.node.internal.findCordapp
import org.junit.Test
import java.util.concurrent.Future

class DummyStateDriverBasedTest {
    private val issuer = TestIdentity(CordaX500Name("Issuer", "", "GB"))
    private val alice = TestIdentity(CordaX500Name("Alice", "", "US"))
    private val bob = TestIdentity(CordaX500Name("Bob", "", "US"))

    private val userName = "username"
    private val password = "password"
    private val user = User(userName, password, permissions = setOf("ALL"))

    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (issuerHandle, aliceHandle, bobHandle) = startNodes(issuer, alice, bob)

        val issuerParty = issuerHandle.nodeInfo.legalIdentities[0]
        val issuerClient = CordaRPCClient(issuerHandle.rpcAddress)
        val issuerProxy: CordaRPCOps = issuerClient.start(userName, password).proxy

        val aliceParty = aliceHandle.nodeInfo.legalIdentities[0]
        val aliceClient = CordaRPCClient(aliceHandle.rpcAddress)
        val aliceProxy: CordaRPCOps = aliceClient.start(userName, password).proxy

        val bobParty = bobHandle.nodeInfo.legalIdentities[0]
        val bobClient = CordaRPCClient(bobHandle.rpcAddress)
        val bobProxy: CordaRPCOps = bobClient.start(userName, password).proxy

        var dummyState = issuerProxy.startFlow(
                ::IssueDummyState,
                aliceParty
        ).returnValue.getOrThrow()
        println("Issuance: ${dummyState.ref.txhash}")

        (0..200).forEach {
            dummyState = aliceProxy.startFlow(
                    ::MoveDummyState,
                    dummyState,
                    bobParty
            ).returnValue.getOrThrow()
            println("tx: ${dummyState.ref.txhash}")

            dummyState = bobProxy.startFlow(
                    ::MoveDummyState,
                    dummyState,
                    aliceParty
            ).returnValue.getOrThrow()
            println("tx: ${dummyState.ref.txhash}")
        }

        dummyState = aliceProxy.startFlow(
                ::MoveDummyState,
                dummyState,
                issuerParty
        ).returnValue.getOrThrow()
        println("last tx: ${dummyState.ref.txhash}")
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
            DriverParameters(
                    isDebug = true,
                    startNodesInProcess = true,
                    cordappsForAllNodes = listOf(
                            findCordapp("com.r3.resolution.contracts"),
                            findCordapp("com.r3.resolution.workflows")
                    ),
                    networkParameters = testNetworkParameters(
                            minimumPlatformVersion=6
                    )
            )
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
            .map { startNode(providedName = it.name, rpcUsers = listOf(user), maximumHeapSize = "1536m") }
            .waitForAll()
}
