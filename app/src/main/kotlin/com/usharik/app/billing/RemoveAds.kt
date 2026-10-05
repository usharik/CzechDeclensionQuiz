package com.usharik.app.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Play product id of the one-time "remove ads" purchase (Play Console → Monetize → One-time products). */
const val REMOVE_ADS_PRODUCT_ID = "remove_ads"

/**
 * The "ads removed" entitlement. It is cached in preferences so an ad-free start never flashes a
 * banner while Play is still being queried; [PurchaseManager] keeps it in sync with Play.
 */
class AdFreeEntitlement(initial: Boolean, private val persist: (Boolean) -> Unit) {
    private val state = MutableStateFlow(initial)
    val adFree: StateFlow<Boolean> = state.asStateFlow()

    fun isAdFree(): Boolean = state.value

    fun set(adFree: Boolean) {
        if (state.value == adFree) return
        state.value = adFree
        persist(adFree)
    }
}

/** What the settings screen shows for the "remove ads" offer. */
data class RemoveAdsOffer(
    /** Localized price from Play, or null while unknown or when billing is unavailable. */
    val formattedPrice: String? = null,
    /** A purchase was made but the payment has not cleared yet (e.g. cash at a store). */
    val pending: Boolean = false,
)

interface PurchaseManager {
    val offer: StateFlow<RemoveAdsOffer>

    /** Connects to Play and syncs the entitlement; safe to call repeatedly (e.g. from onResume). */
    fun refresh()

    /** Opens the Play purchase sheet; returns false when the product is not loaded yet. */
    fun launchRemoveAdsPurchase(activity: Activity): Boolean
}

/** A Play purchase reduced to the fields the entitlement logic needs. */
data class PurchaseSnapshot(
    val products: List<String>,
    val purchased: Boolean,
    val pending: Boolean,
    val acknowledged: Boolean,
    val token: String,
)

/** The outcome of a full purchase query: whether ads are removed and which purchases still need acknowledging. */
data class RemoveAdsStatus(val owned: Boolean, val pending: Boolean, val tokensToAcknowledge: List<String>)

object RemoveAdsPurchases {
    fun evaluate(purchases: List<PurchaseSnapshot>): RemoveAdsStatus {
        val removeAds = purchases.filter { REMOVE_ADS_PRODUCT_ID in it.products }
        val owned = removeAds.filter { it.purchased }
        return RemoveAdsStatus(
            owned = owned.isNotEmpty(),
            pending = owned.isEmpty() && removeAds.any { it.pending },
            // Play refunds a purchase that is not acknowledged within three days.
            tokensToAcknowledge = owned.filterNot { it.acknowledged }.map { it.token },
        )
    }
}
