package com.runcheck.domain.repository

fun interface DatabaseTransactionRunner {
    suspend fun runInTransaction(block: suspend () -> Unit)
}
