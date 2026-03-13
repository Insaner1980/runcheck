package com.devicepulse.data.db

import androidx.room.withTransaction
import com.devicepulse.domain.repository.DatabaseTransactionRunner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: DevicePulseDatabase
) : DatabaseTransactionRunner {
    override suspend fun runInTransaction(block: suspend () -> Unit) {
        database.withTransaction {
            block()
        }
    }
}
