package io.github.jqssun.airplay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.billing.Billing
import io.github.jqssun.airplay.billing.SessionLimiter

/**
 * Screenbridge Unlimited: Monthly (with Play's free trial when this account may use it) or Lifetime.
 * [expired] = the free 20 minutes just ran out; otherwise opened from Settings. Monthly has D-pad focus;
 * Back or "Not now" closes it.
 */
@Composable
fun PaywallScreen(
    expired: Boolean,
    monthly: Billing.MonthlyOffer?,
    lifetimePrice: String?,
    pending: Boolean,
    onBuy: (productId: String) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val c = MaterialTheme.colorScheme
        val firstFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { firstFocus.requestFocusUntilLanded() }
        // a dialog has no Surface of its own: without one the text falls back to black
        Surface(Modifier.fillMaxSize(), color = c.background, contentColor = c.onBackground) { Box(
            Modifier
                .fillMaxSize()
                // two soft glows instead of a flat wall of grey
                .background(Brush.radialGradient(listOf(c.primary.copy(alpha = 0.22f), Color.Transparent), center = Offset(0f, 0f), radius = 1400f))
                .background(Brush.radialGradient(listOf(c.tertiary.copy(alpha = 0.14f), Color.Transparent), center = Offset(2400f, 1400f), radius = 1300f))
        ) {
            Row(
                Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 56.dp, vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.ic_launcher_art), contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(R.string.unlimited_title), style = MaterialTheme.typography.titleMedium,
                            color = c.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(if (expired) R.string.paywall_title else R.string.paywall_title_plans),
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(if (expired) R.string.paywall_subtitle else R.string.paywall_subtitle_plans),
                        style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(28.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Benefit(Icons.Rounded.AllInclusive, stringResource(R.string.paywall_benefit_unlimited))
                        Benefit(Icons.Rounded.Update, stringResource(R.string.paywall_benefit_updates))
                        Benefit(Icons.Rounded.Favorite, stringResource(R.string.paywall_benefit_support))
                    }
                }

                Column(Modifier.width(430.dp)) {
                    val trial = monthly?.trialDays
                    PlanCard(
                        name = stringResource(R.string.plan_monthly),
                        badge = trial?.let { pluralStringResource(R.plurals.plan_trial_badge, it, it) },
                        detail = when {
                            monthly == null -> stringResource(R.string.plan_price_from_play)
                            trial != null -> pluralStringResource(R.plurals.plan_monthly_trial_detail, trial, trial, monthly.price)
                            else -> stringResource(R.string.plan_monthly_detail)
                        },
                        price = if (trial != null) stringResource(R.string.plan_free) else monthly?.price,
                        unit = if (trial != null) pluralStringResource(R.plurals.plan_for_days, trial, trial) else stringResource(R.string.plan_per_month),
                        onClick = { onBuy(SessionLimiter.MONTHLY_ID) },
                        modifier = Modifier.focusRequester(firstFocus),
                    )
                    Spacer(Modifier.height(16.dp))
                    PlanCard(
                        name = stringResource(R.string.plan_lifetime),
                        badge = stringResource(R.string.plan_lifetime_badge),
                        detail = stringResource(if (lifetimePrice == null) R.string.plan_price_from_play else R.string.plan_lifetime_detail),
                        price = lifetimePrice,
                        unit = stringResource(R.string.plan_one_time),
                        onClick = { onBuy(SessionLimiter.LIFETIME_ID) },
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onClose, modifier = Modifier.dpadFocus(CircleShape)) {
                            Text(stringResource(R.string.paywall_not_now), style = MaterialTheme.typography.titleSmall)
                        }
                        TextButton(onClick = onRestore, modifier = Modifier.dpadFocus(CircleShape)) {
                            Text(stringResource(R.string.unlimited_restore), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            pending -> stringResource(R.string.unlimited_pending_desc)
                            monthly != null -> stringResource(R.string.paywall_terms, monthly.price)
                            else -> stringResource(R.string.paywall_terms_no_price)
                        },
                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                    )
                }
            }
        } }
    }
}

@Composable
private fun Benefit(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** A plan you can pick with the remote: lifts and lights up while focused. */
@Composable
private fun PlanCard(
    name: String,
    badge: String?,
    detail: String,
    price: String?,
    unit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "plan")
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = RoundedCornerShape(20.dp),
        color = if (focused) c.primaryContainer else c.surfaceContainerHigh,
        contentColor = if (focused) c.onPrimaryContainer else c.onSurface,
        border = if (focused) BorderStroke(3.dp, c.primary) else BorderStroke(1.dp, c.outlineVariant),
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            badge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            color = if (focused) c.onPrimary else c.onTertiaryContainer,
                            modifier = Modifier
                                .background(if (focused) c.primary else c.tertiaryContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium,
                    color = if (focused) c.onPrimaryContainer else c.onSurfaceVariant)
            }
            if (price != null) {
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(price, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(unit, style = MaterialTheme.typography.bodySmall,
                        color = if (focused) c.onPrimaryContainer else c.onSurfaceVariant)
                }
            }
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
