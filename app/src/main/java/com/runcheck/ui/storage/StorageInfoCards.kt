package com.runcheck.ui.storage

/**
 * Info card IDs for the Storage detail screen.
 *
 * Card IDs include a version suffix (e.g. "_v1"). When card content is updated
 * in a future release, bump the version so that previously dismissed cards
 * resurface with the new content.
 */
object StorageInfoCards {
    const val FULL_STORAGE_SLOW = "storage_full_slow_v1"
    const val STORAGE_OVERVIEW = "storage_overview_v1"
}
