package com.r3.resolution.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class DummyStateContract: Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Issue -> verifyIssueCommand(tx, command)
            is Commands.Move -> verifyMoveCommand(tx, command)
            else -> throw IllegalArgumentException("Command ${command.value} not supported")
        }
    }

    private fun verifyIssueCommand(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {  }
    }

    private fun verifyMoveCommand(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {  }
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
    }
}
