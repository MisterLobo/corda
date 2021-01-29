package com.r3.resolution.workflows

import org.junit.Test
import java.io.File

class Transactions {

    @Test
    fun x() {
        File("/Users/agnieszkaszczepanska/Documents/transactions.txt").forEachLine {
            if(it.startsWith("Issuance: ") or it.startsWith("tx:"))
            println(it)
        }
    }

}