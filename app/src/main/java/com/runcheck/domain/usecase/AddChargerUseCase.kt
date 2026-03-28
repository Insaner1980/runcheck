package com.runcheck.domain.usecase

import com.runcheck.domain.repository.ChargerRepository
import javax.inject.Inject

class AddChargerUseCase
    @Inject
    constructor(
        private val chargerRepository: ChargerRepository,
    ) {
        suspend operator fun invoke(name: String): Long = chargerRepository.insertCharger(name)
    }
