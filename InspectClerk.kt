package com.oryno.piggy_ledger

import com.clerk.api.Clerk

fun inspect() {
    val methods = Clerk.sessionFlow.value?.javaClass?.methods
    methods?.forEach {
        println(it.name)
    }
}
