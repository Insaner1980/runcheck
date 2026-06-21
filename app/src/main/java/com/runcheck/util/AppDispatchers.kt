package com.runcheck.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDispatchers
    @Inject
    constructor() {
        val io: CoroutineDispatcher
            get() = Dispatchers.IO

        val default: CoroutineDispatcher
            get() = Dispatchers.Default

        val main: CoroutineDispatcher
            get() = Dispatchers.Main

        val mainImmediate: CoroutineDispatcher
            get() = Dispatchers.Main.immediate
    }
