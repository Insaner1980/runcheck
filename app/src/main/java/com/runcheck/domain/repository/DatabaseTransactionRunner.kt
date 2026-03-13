package com.devicepulse.domain.repository

fun interface DatabaseTransactionRunner {
    suspend fun runInTransaction(block: suspend () -> Unit)
}
