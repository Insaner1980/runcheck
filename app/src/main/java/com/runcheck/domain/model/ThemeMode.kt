package com.runcheck.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStoredValue(value: String?): ThemeMode =
            entries.firstOrNull { mode -> mode.name == value } ?: SYSTEM
    }
}
