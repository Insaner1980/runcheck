package com.runcheck.domain.repository

import kotlinx.coroutines.flow.Flow

interface InfoCardDismissalRepository {
    fun observeDismissedCardIds(): Flow<Set<String>>
    suspend fun dismissCard(cardId: String)
    suspend fun resetDismissedCards()
}
