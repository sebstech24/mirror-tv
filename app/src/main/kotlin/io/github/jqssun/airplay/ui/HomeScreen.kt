package io.github.jqssun.airplay.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.billing.Billing
import java.net.Inet4Address

// IPv4 of the active network; needs only ACCESS_NETWORK_STATE (Wi-Fi name would need location, so it's skipped)
fun Context.tvIpv4(): String? = runCatching {
    val cm = getSystemService(ConnectivityManager::class.java) ?: return null
    cm.getLinkProperties(cm.activeNetwork)?.linkAddresses
        ?.map { it.address }
        ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        ?.hostAddress
}.getOrNull()

enum class HomeStatus { READY, OFF, ERROR, NEEDS_ACCESS }

/** Full-screen, calm waiting screen: TV name, status, the exact clicks on a Mac. */
@Composable
fun WaitingContent(
    tvName: String,
    status: HomeStatus,
    address: String?,
    noticeTitle: String?,
    noticeText: String?,
    actionLabel: String?,
    onAction: () -> Unit,
    actionFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .fillMaxWidth()
            // ~5% TV overscan-safe margins
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val unlimited by Billing.unlocked.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(status)
            if (unlimited) {
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.unlimited_badge),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = tvName,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (noticeTitle != null) {
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 720.dp),
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(noticeTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    if (noticeText != null) Text(
                        noticeText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction, modifier = Modifier.focusRequester(actionFocus).dpadFocus(CircleShape)) {
                Text(actionLabel, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (status == HomeStatus.NEEDS_ACCESS) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.local_network_desc),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (status == HomeStatus.READY) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.home_mac_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            MacSteps(tvName)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.home_iphone, tvName),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_mirror_hint) + "\n" + stringResource(R.string.home_screensaver_hint),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = listOfNotNull(
                stringResource(R.string.home_same_wifi),
                address?.let { stringResource(R.string.home_tv_address, it) },
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
    }
}

@Composable
private fun StatusPill(status: HomeStatus) {
    val ok = status == HomeStatus.READY
    val dot = if (ok) Color(0xFF34C759) else MaterialTheme.colorScheme.error
    val label = when (status) {
        HomeStatus.READY -> R.string.status_ready
        HomeStatus.OFF -> R.string.status_off
        HomeStatus.ERROR -> R.string.status_error
        HomeStatus.NEEDS_ACCESS -> R.string.local_network_label
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(14.dp).background(dot, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MacSteps(tvName: String) {
    val steps: List<Pair<Painter, String>> = listOf(
        painterResource(R.drawable.ic_control_center) to stringResource(R.string.home_step1),
        rememberVectorPainter(Icons.AutoMirrored.Filled.ScreenShare) to stringResource(R.string.home_step2),
        rememberVectorPainter(Icons.Default.Tv) to stringResource(R.string.home_step3, tvName),
    )
    BoxWithConstraints(Modifier.widthIn(max = 1100.dp)) {
        val wide = maxWidth >= 600.dp
        val cards: @Composable (Modifier) -> Unit = { m ->
            steps.forEachIndexed { i, (icon, text) -> StepCard(i + 1, icon, text, m) }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                cards(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { cards(Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun StepCard(number: Int, icon: Painter, text: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$number", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}

/** Shown while a sender has connected but no picture has arrived yet. */
@Composable
fun ConnectingContent(clientName: String?, takingOver: Boolean, modifier: Modifier = Modifier) {
    val title = when {
        clientName == null -> stringResource(R.string.home_connecting)
        takingOver -> stringResource(R.string.home_taking_over, clientName)
        else -> stringResource(R.string.home_connecting_named, clientName)
    }
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.home_connecting_hint),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF101114)
@Composable
private fun WaitingTvPreview() = MaterialTheme(colorScheme = darkColorScheme()) {
    Surface {
        WaitingContent("Living Room TV", HomeStatus.READY, "192.168.1.20", noticeTitle = null, noticeText = null,
            actionLabel = null, onAction = {}, actionFocus = FocusRequester())
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true, backgroundColor = 0xFF101114)
@Composable
private fun StoppedTvPreview() = MaterialTheme(colorScheme = darkColorScheme()) {
    Surface {
        WaitingContent("Living Room TV", HomeStatus.READY, "192.168.1.20", noticeTitle = "Connection lost — check Wi‑Fi",
            noticeText = "Make sure the Mac and TV are on the same Wi‑Fi, then choose “Living Room TV” again.",
            actionLabel = null, onAction = {}, actionFocus = FocusRequester())
    }
}

@Preview(device = Devices.PIXEL_7, showBackground = true)
@Composable
private fun WaitingPhonePreview() = MaterialTheme {
    Surface {
        WaitingContent("Living Room TV", HomeStatus.OFF, null, noticeTitle = null, noticeText = null,
            actionLabel = "Turn on", onAction = {}, actionFocus = FocusRequester())
    }
}
