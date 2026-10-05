package com.usharik.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveAdsPurchasesTest {
    private fun purchase(
        product: String = REMOVE_ADS_PRODUCT_ID,
        purchased: Boolean = true,
        pending: Boolean = false,
        acknowledged: Boolean = true,
        token: String = "t",
    ) = PurchaseSnapshot(listOf(product), purchased, pending, acknowledged, token)

    @Test fun noPurchasesMeansAdsStay() {
        assertEquals(RemoveAdsStatus(owned = false, pending = false, tokensToAcknowledge = emptyList()), RemoveAdsPurchases.evaluate(emptyList()))
    }

    @Test fun purchasedProductRemovesAdsAndNeedsAcknowledgingOnce() {
        val status = RemoveAdsPurchases.evaluate(listOf(purchase(acknowledged = false, token = "new")))
        assertTrue(status.owned)
        assertEquals(listOf("new"), status.tokensToAcknowledge)
        assertEquals(emptyList<String>(), RemoveAdsPurchases.evaluate(listOf(purchase())).tokensToAcknowledge)
    }

    @Test fun pendingPaymentDoesNotRemoveAdsYet() {
        val status = RemoveAdsPurchases.evaluate(listOf(purchase(purchased = false, pending = true, acknowledged = false)))
        assertFalse(status.owned)
        assertTrue(status.pending)
        assertEquals(emptyList<String>(), status.tokensToAcknowledge)
    }

    @Test fun otherProductsAreIgnored() {
        assertFalse(RemoveAdsPurchases.evaluate(listOf(purchase(product = "something_else"))).owned)
    }

    @Test fun entitlementPersistsOnlyChanges() {
        val persisted = mutableListOf<Boolean>()
        val entitlement = AdFreeEntitlement(false) { persisted += it }
        entitlement.set(false)
        entitlement.set(true)
        entitlement.set(true)
        assertTrue(entitlement.adFree.value)
        assertEquals(listOf(true), persisted)
    }
}
