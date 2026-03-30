package com.runcheck.util

import androidx.lifecycle.SavedStateHandle

inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String?,
    default: T,
): T = value?.let { raw -> enumValues<T>().firstOrNull { it.name == raw } } ?: default

inline fun <reified T : Enum<T>> SavedStateHandle.getEnumOrDefault(
    key: String,
    default: T,
): T = enumValueOrDefault(get<String>(key), default)

fun SavedStateHandle.getBooleanOrDefault(
    key: String,
    default: Boolean,
): Boolean = get<Boolean>(key) ?: default

fun SavedStateHandle.getIntOrDefault(
    key: String,
    default: Int,
): Int = get<Int>(key) ?: default

fun SavedStateHandle.getSanitizedString(
    key: String,
    sanitize: (String?) -> String,
): String {
    val sanitized = sanitize(get<String>(key))
    this[key] = sanitized
    return sanitized
}

fun SavedStateHandle.putEnum(
    key: String,
    value: Enum<*>,
) {
    this[key] = value.name
}
