package com.usharik.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.usharik.app.App
import com.usharik.app.BuildConfig
import com.usharik.app.R
import com.usharik.app.ui.components.GradientButton
import com.usharik.app.ui.components.OutlinedModernButton
import com.usharik.app.ui.theme.AppColors
import com.usharik.app.ui.theme.Dimens
import com.usharik.app.utils.HapticFeedback
import java.text.DateFormat
import java.util.Date

/**
 * About page. Faithful Compose port of AboutFragment + about_fragment.xml: centered logo,
 * app name, version/build info and the rate/privacy buttons (plus the debug-only test
 * notification button).
 */
@Composable
fun AboutScreen(app: App) {
    val context = LocalContext.current

    fun rateApp() {
        HapticFeedback.light(context)
        val packageName = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(context, R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spacingMd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(R.mipmap.ic_launcher_round),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.graphicsLayer { scaleX = 1.5f; scaleY = 1.5f },
        )
        Text(
            stringResource(R.string.app_name),
            Modifier.padding(top = Dimens.spacingXxl),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
        )
        Text(
            stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.GIT_COMMIT_HASH),
            Modifier.padding(top = Dimens.spacingXs),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
        )
        Text(
            stringResource(R.string.date_of_build, DateFormat.getInstance().format(Date(BuildConfig.TIMESTAMP))),
            Modifier.padding(top = Dimens.spacingContent),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = Dimens.textBody,
        )
        GradientButton(
            text = stringResource(R.string.rate_app),
            gradient = AppColors.gradientAccent,
            icon = painterResource(R.drawable.ic_star),
            fontSize = Dimens.textBody,
            onClick = ::rateApp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingButtonHorizontal)
                .padding(top = Dimens.spacingXl),
        )
        OutlinedModernButton(
            text = stringResource(R.string.privacy_policy),
            icon = painterResource(R.drawable.ic_privacy_black_24dp),
            fontSize = Dimens.textBody,
            onClick = {
                HapticFeedback.light(context)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://raw.githubusercontent.com/usharik/CzechDeclensionQuiz/refs/heads/main/privacy_policy.md")),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingButtonHorizontal)
                .padding(top = Dimens.spacingMd),
        )
        if (BuildConfig.DEBUG) {
            OutlinedModernButton(
                text = stringResource(R.string.test_notification),
                icon = painterResource(R.drawable.ic_notifications_black_24dp),
                fontSize = Dimens.textBody,
                onClick = {
                    HapticFeedback.light(context)
                    app.notificationHelper.showDailyReminder(context, false, 1, 0, 0)
                    Toast.makeText(context, R.string.test_notification_sent, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingButtonHorizontal)
                    .padding(top = Dimens.spacingMd),
            )
        }
    }
}
