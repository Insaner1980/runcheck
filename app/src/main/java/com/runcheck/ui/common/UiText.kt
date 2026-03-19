package com.runcheck.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext

/**
 * Encapsulates a text value that may come from a string resource or
 * a dynamic string. Allows ViewModels to produce user-visible text
 * without holding a Context reference.
 */
@Stable
sealed interface UiText {
    data class Resource(@param:StringRes val id: Int) : UiText
    data class Dynamic(val value: String) : UiText

    fun resolve(context: Context): String = when (this) {
        is Resource -> context.getString(id)
        is Dynamic -> value
    }
}

@Composable
fun UiText.resolve(): String {
    val context = LocalContext.current
    return resolve(context)
}
