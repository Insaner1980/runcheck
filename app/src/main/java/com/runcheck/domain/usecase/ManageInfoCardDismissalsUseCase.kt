package com.runcheck.domain.usecase

import com.runcheck.domain.repository.InfoCardDismissalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageInfoCardDismissalsUseCase
    @Inject
    constructor(
        private val infoCardDismissalRepository: InfoCardDismissalRepository,
    ) {
        fun observeDismissedCardIds(): Flow<Set<String>> = infoCardDismissalRepository.observeDismissedCardIds()

        suspend fun dismissCard(cardId: String) {
            infoCardDismissalRepository.dismissCard(cardId)
        }

        suspend fun resetDismissedCards() {
            infoCardDismissalRepository.resetDismissedCards()
        }
    }
