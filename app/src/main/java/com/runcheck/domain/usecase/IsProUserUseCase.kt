package com.runcheck.domain.usecase

import com.runcheck.domain.repository.ProStatusProvider
import javax.inject.Inject

class IsProUserUseCase
    @Inject
    constructor(
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(): Boolean = proStatusProvider.isPro()
    }
