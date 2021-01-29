package com.r3.resolution.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(DummyStateContract::class)
class DummyState(
        val issuer: Party,
        val owner: Party
): ContractState {
    private val random = Random(0)
    private val l = (0..10000).map { SecureHash.randomSHA256() }
    override val participants: List<AbstractParty>
        get() = listOf(owner)
}
