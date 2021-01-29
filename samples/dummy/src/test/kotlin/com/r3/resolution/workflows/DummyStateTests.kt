package com.r3.resolution.workflows

import org.junit.Test

class DummyStateTests: AbstractFlowTest() {

    @Test
    fun `Test`() {
        var state = issueDummyState(issuerNode, aliceParty)
        (0..1500).forEach {
            state = moveDummyState(aliceNode, state, bobParty)
            state = moveDummyState(bobNode, state, aliceParty)
        }
        println("ok")
        val issuerState = moveDummyState(aliceNode, state, issuerParty)
        println(issuerState)
    }
}
