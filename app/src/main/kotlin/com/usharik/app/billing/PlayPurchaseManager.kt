package com.usharik.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Google Play Billing for the one-time [REMOVE_ADS_PRODUCT_ID] purchase. There is no backend, so the
 * entitlement follows Play's own purchase list: a full query grants or revokes it (refunds included),
 * a purchase update only grants it. Billing callbacks arrive on the main thread. [logEvent] receives
 * the purchase funnel's analytics events.
 */
class PlayPurchaseManager(
    context: Context,
    private val entitlement: AdFreeEntitlement,
    private val logEvent: (String) -> Unit = {},
) : PurchaseManager {
    private val state = MutableStateFlow(RemoveAdsOffer())
    override val offer: StateFlow<RemoveAdsOffer> = state.asStateFlow()

    private var productDetails: ProductDetails? = null
    private var connecting = false

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases.orEmpty()) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun refresh() {
        if (client.isReady) return syncWithPlay()
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingResponseCode.OK) return syncWithPlay()
                Log.w(TAG, "Billing setup failed: ${result.describe()}")
                state.update { it.copy(billingChecked = true) }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
            }
        })
    }

    override fun launchRemoveAdsPurchase(activity: Activity): Boolean {
        val details = productDetails ?: run { refresh(); return false }
        val offerToken = details.oneTimePurchaseOfferDetails?.offerToken ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) Log.w(TAG, "Purchase flow not started: ${result.describe()}")
        return result.responseCode == BillingResponseCode.OK
    }

    private fun syncWithPlay() {
        if (productDetails == null) queryProduct()
        queryPurchases()
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(REMOVE_ADS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, details ->
            if (result.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "Product query failed: ${result.describe()}")
                state.update { it.copy(billingChecked = true) }
                return@queryProductDetailsAsync
            }
            val removeAds = details.productDetailsList.firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID }
            productDetails = removeAds
            state.update { it.copy(formattedPrice = removeAds?.oneTimePurchaseOfferDetails?.formattedPrice, billingChecked = true) }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "Purchase query failed: ${result.describe()}")
                return@queryPurchasesAsync
            }
            val status = RemoveAdsPurchases.evaluate(purchases.map { it.snapshot() })
            if (status.owned && !entitlement.isAdFree()) logEvent("remove_ads_restored")
            apply(status, fullList = true)
            state.update { it.copy(purchasesSynced = true) }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                val status = RemoveAdsPurchases.evaluate(purchases.map { it.snapshot() })
                when {
                    status.owned && !entitlement.isAdFree() -> logEvent("remove_ads_purchased")
                    status.pending -> logEvent("remove_ads_pending")
                }
                apply(status, fullList = false)
            }
            // Bought on another device or before a reinstall: the full query restores it.
            BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
            BillingResponseCode.USER_CANCELED -> logEvent("remove_ads_cancelled")
            else -> {
                Log.w(TAG, "Purchase failed: ${result.describe()}")
                logEvent("remove_ads_failed")
            }
        }
    }

    private fun apply(status: RemoveAdsStatus, fullList: Boolean) {
        if (status.owned || fullList) entitlement.set(status.owned)
        state.update { it.copy(pending = status.pending) }
        status.tokensToAcknowledge.forEach { token ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()) { ack ->
                if (ack.responseCode != BillingResponseCode.OK) Log.w(TAG, "Acknowledge failed: ${ack.describe()}")
            }
        }
    }

    private fun Purchase.snapshot() = PurchaseSnapshot(
        products = products,
        purchased = purchaseState == Purchase.PurchaseState.PURCHASED,
        pending = purchaseState == Purchase.PurchaseState.PENDING,
        acknowledged = isAcknowledged,
        token = purchaseToken,
    )

    private fun BillingResult.describe() = "$responseCode $debugMessage"

    private companion object {
        const val TAG = "PlayPurchaseManager"
    }
}
