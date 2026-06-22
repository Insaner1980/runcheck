package com.runcheck.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AppDispatchers
    @Inject
    constructor() {
        open val io: CoroutineDispatcher
            get() = Dispatchers.IO

        open val default: CoroutineDispatcher
            get() = Dispatchers.Default

        open val main: CoroutineDispatcher
            get() = Dispatchers.Main

        open val mainImmediate: CoroutineDispatcher
            get() = Dispatchers.Main.immediate
    }
