package com.runcheck.ui.network

/**
 * Info card IDs for the Network detail screen.
 *
 * Card IDs include a version suffix (e.g. "_v1"). When card content is updated
 * in a future release, bump the version so that previously dismissed cards
 * resurface with the new content.
 */
object NetworkInfoCards {
    const val WEAK_SIGNAL_DRAIN = "network_weak_signal_drain_v1"
    const val SPEED_TEST_INFO = "network_speed_test_info_v1"
}
