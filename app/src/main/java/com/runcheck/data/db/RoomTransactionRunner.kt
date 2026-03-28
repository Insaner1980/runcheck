package com.runcheck.data.db

import androidx.room.withTransaction
import com.runcheck.domain.repository.DatabaseTransactionRunner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransactionRunner
    @Inject
    constructor(
        private val database: RuncheckDatabase,
    ) : DatabaseTransactionRunner {
        override suspend fun runInTransaction(block: suspend () -> Unit) {
            database.withTransaction {
                block()
            }
        }
    }
