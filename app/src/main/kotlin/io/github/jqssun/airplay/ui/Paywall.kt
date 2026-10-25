package io.github.jqssun.airplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.R

/** Shown when a free session ends. "Unlock" has D-pad focus; "Not now" and Back return Home. */
@Composable
fun PaywallScreen(price: String?, pending: Boolean, onUnlock: () -> Unit, onNotNow: () -> Unit) {
    BackHandler(onBack = onNotNow)
    val unlockFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { unlockFocus.requestFocusUntilLanded() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 760.dp),
            )
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(R.string.paywall_benefit_unlimited, R.string.paywall_benefit_support, R.string.paywall_benefit_updates).forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(it), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onUnlock, modifier = Modifier.focusRequester(unlockFocus).dpadFocus(CircleShape)) {
                    Text(
                        price?.let { stringResource(R.string.paywall_unlock, it) } ?: stringResource(R.string.paywall_unlock_no_price),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(onClick = onNotNow, modifier = Modifier.dpadFocus(CircleShape)) {
                    Text(stringResource(R.string.paywall_not_now), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(if (pending) R.string.unlimited_pending_desc else R.string.paywall_one_time),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Small, non-focusable notice near the end of a free session. */
@Composable
fun FreeSessionNotice(remainingMs: Long?, modifier: Modifier = Modifier) {
    if (remainingMs == null) return
    val minutes = ((remainingMs + 59_999) / 60_000).toInt()
    Text(
        pluralStringResource(R.plurals.free_session_left, minutes, minutes),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = modifier
            .safeDrawingPadding()
            .padding(bottom = 32.dp)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    )
}
