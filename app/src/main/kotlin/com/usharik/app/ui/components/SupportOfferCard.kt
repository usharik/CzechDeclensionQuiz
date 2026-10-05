package com.usharik.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.usharik.app.R
import com.usharik.app.TestTags

/**
 * Hub invitation to buy the ad-free version and support the developer. Shown only when
 * [com.usharik.app.billing.SupportOfferPolicy] allows it; both dismiss actions are always visible.
 */
@Composable
fun SupportOfferCard(price: String, onBuy: () -> Unit, onNotNow: () -> Unit, onNeverShow: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth().testTag(TestTags.HUB_SUPPORT_OFFER),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
            Text(stringResource(R.string.support_offer_title), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.support_offer_body),
                Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onBuy, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag(TestTags.HUB_SUPPORT_OFFER_BUY)) {
                Text(stringResource(R.string.support_offer_buy, price))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onNeverShow, modifier = Modifier.testTag(TestTags.HUB_SUPPORT_OFFER_NEVER)) {
                    Text(stringResource(R.string.support_offer_never))
                }
                TextButton(onClick = onNotNow, modifier = Modifier.testTag(TestTags.HUB_SUPPORT_OFFER_LATER)) {
                    Text(stringResource(R.string.support_offer_later))
                }
            }
        }
    }
}
