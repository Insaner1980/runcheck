package com.runcheck.ui.network

import com.runcheck.R
import com.runcheck.ui.components.info.InfoSheetContent

object NetworkInfoContent {
    val signalStrength = InfoSheetContent(
        title = R.string.info_network_signal_strength_title,
        explanation = R.string.info_network_signal_strength_explanation,
        normalRange = R.string.info_network_signal_strength_range,
        whyItMatters = R.string.info_network_signal_strength_matters,
        deeperDetail = R.string.info_network_signal_strength_detail
    )
    val latency = InfoSheetContent(
        title = R.string.info_network_latency_title,
        explanation = R.string.info_network_latency_explanation,
        normalRange = R.string.info_network_latency_range,
        whyItMatters = R.string.info_network_latency_matters
    )
    val jitter = InfoSheetContent(
        title = R.string.info_network_jitter_title,
        explanation = R.string.info_network_jitter_explanation,
        normalRange = R.string.info_network_jitter_range,
        whyItMatters = R.string.info_network_jitter_matters
    )
    val frequency = InfoSheetContent(
        title = R.string.info_network_frequency_title,
        explanation = R.string.info_network_frequency_explanation,
        normalRange = R.string.info_network_frequency_range,
        whyItMatters = R.string.info_network_frequency_matters
    )
    val wifiStandard = InfoSheetContent(
        title = R.string.info_network_wifi_standard_title,
        explanation = R.string.info_network_wifi_standard_explanation,
        normalRange = R.string.info_network_wifi_standard_range,
        whyItMatters = R.string.info_network_wifi_standard_matters
    )
    val linkSpeed = InfoSheetContent(
        title = R.string.info_network_link_speed_title,
        explanation = R.string.info_network_link_speed_explanation,
        normalRange = R.string.info_network_link_speed_range,
        whyItMatters = R.string.info_network_link_speed_matters
    )
    val bandwidth = InfoSheetContent(
        title = R.string.info_network_bandwidth_title,
        explanation = R.string.info_network_bandwidth_explanation,
        normalRange = R.string.info_network_bandwidth_range,
        whyItMatters = R.string.info_network_bandwidth_matters
    )
    val mtu = InfoSheetContent(
        title = R.string.info_network_mtu_title,
        explanation = R.string.info_network_mtu_explanation,
        normalRange = R.string.info_network_mtu_range,
        whyItMatters = R.string.info_network_mtu_matters
    )
    val connectionType = InfoSheetContent(
        title = R.string.info_network_connection_type_title,
        explanation = R.string.info_network_connection_type_explanation,
        normalRange = R.string.info_network_connection_type_range,
        whyItMatters = R.string.info_network_connection_type_matters
    )
    val metered = InfoSheetContent(
        title = R.string.info_network_metered_title,
        explanation = R.string.info_network_metered_explanation,
        normalRange = R.string.info_network_metered_range,
        whyItMatters = R.string.info_network_metered_matters
    )
    val roaming = InfoSheetContent(
        title = R.string.info_network_roaming_title,
        explanation = R.string.info_network_roaming_explanation,
        normalRange = R.string.info_network_roaming_range,
        whyItMatters = R.string.info_network_roaming_matters
    )
    val vpn = InfoSheetContent(
        title = R.string.info_network_vpn_title,
        explanation = R.string.info_network_vpn_explanation,
        normalRange = R.string.info_network_vpn_range,
        whyItMatters = R.string.info_network_vpn_matters
    )
    val subtype = InfoSheetContent(
        title = R.string.info_network_subtype_title,
        explanation = R.string.info_network_subtype_explanation,
        normalRange = R.string.info_network_subtype_range,
        whyItMatters = R.string.info_network_subtype_matters
    )
}
