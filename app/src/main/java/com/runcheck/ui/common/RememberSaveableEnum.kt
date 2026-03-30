package com.runcheck.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.runcheck.util.enumValueOrDefault

@Composable
inline fun <reified T : Enum<T>> rememberSaveableEnumState(default: T): MutableState<T> {
    val saver =
        remember(default) {
            Saver<MutableState<T>, String>(
                save = { it.value.name },
                restore = { mutableStateOf(enumValueOrDefault(it, default)) },
            )
        }
    return rememberSaveable(saver = saver) { mutableStateOf(default) }
}
