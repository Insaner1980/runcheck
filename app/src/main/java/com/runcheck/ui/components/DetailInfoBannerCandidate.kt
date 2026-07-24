package com.runcheck.ui.components

data class DetailInfoBannerCandidate(
    val id: String,
    val severity: Int,
    val catalogOrder: Int,
    val eligible: Boolean = true,
)

fun selectDetailInfoBanner(
    candidates: List<DetailInfoBannerCandidate>,
    dismissedIds: Set<String>,
    showInfoBanners: Boolean,
): DetailInfoBannerCandidate? {
    if (!showInfoBanners) return null

    return candidates
        .asSequence()
        .filter(DetailInfoBannerCandidate::eligible)
        .filterNot { it.id in dismissedIds }
        .sortedWith(
            compareByDescending(DetailInfoBannerCandidate::severity)
                .thenBy(DetailInfoBannerCandidate::catalogOrder),
        ).firstOrNull()
}
