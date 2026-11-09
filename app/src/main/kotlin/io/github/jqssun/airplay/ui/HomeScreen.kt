package io.github.jqssun.airplay.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import io.github.jqssun.airplay.R
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

// tvOS-like palette for the waiting screen: pure black, white text at Apple's label opacities, system blue/green
private val Label = Color.White
private val Secondary = Color(0x99EBEBF5)
private val Tertiary = Color(0x4DEBEBF5)
private val Glass = Color.White.copy(alpha = 0.06f)
private val GlassEdge = Color.White.copy(alpha = 0.10f)
private val SystemBlue = Color(0xFF0A84FF)
private val SystemGreen = Color(0xFF30D158)

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
    // soft light from above, like the Apple TV home screen; drawn behind the whole screen
    Box(Modifier.fillMaxSize().background(Color.Black).drawBehind {
        drawRect(Brush.radialGradient(
            listOf(Color(0xFF1C3460).copy(alpha = 0.6f), Color.Transparent),
            center = Offset(size.width / 2, 0f),
            radius = size.width * 0.65f,
        ))
    })
    // TVs crop a few percent off every edge (overscan): keep ~6% clear, and never let the block run off screen
    FitToScreen(modifier.fillMaxSize().padding(horizontal = 80.dp, vertical = 40.dp)) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusPill(status)
        Spacer(Modifier.height(20.dp))
        Text(
            text = tvName,
            color = Label,
            fontSize = 60.sp,
            lineHeight = 64.sp,
            letterSpacing = (-1.2).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (noticeTitle != null) {
            Spacer(Modifier.height(28.dp))
            GlassPanel(Modifier.widthIn(max = 760.dp)) {
                Column(Modifier.padding(horizontal = 36.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(noticeTitle, color = Label, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, textAlign = TextAlign.Center)
                    if (noticeText != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(noticeText, color = Secondary, fontSize = 19.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (actionLabel != null) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 14.dp),
                modifier = Modifier.focusRequester(actionFocus).dpadFocus(CircleShape),
            ) {
                Text(actionLabel, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (status == HomeStatus.NEEDS_ACCESS) {
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.local_network_desc),
                color = Secondary, fontSize = 19.sp, lineHeight = 26.sp,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 760.dp),
            )
        }

        if (status == HomeStatus.READY) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.home_mac_title), color = Secondary, fontSize = 24.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(36.dp))
            MacSteps(tvName)
            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.home_iphone, tvName),
                color = Secondary, fontSize = 19.sp, lineHeight = 26.sp,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 1000.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.home_mirror_hint) + "\n" + stringResource(R.string.home_screensaver_hint),
                color = Tertiary, fontSize = 15.sp, lineHeight = 22.sp,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 1000.dp),
            )
        }

        Spacer(Modifier.height(if (status == HomeStatus.READY) 8.dp else 28.dp))
        Text(
            text = listOfNotNull(
                stringResource(R.string.home_same_wifi),
                address?.let { stringResource(R.string.home_tv_address, it) },
            ).joinToString("  ·  "),
            color = Tertiary, fontSize = 15.sp, lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.clip(shape).background(Glass).border(1.dp, GlassEdge, shape)) { content() }
}

/** Centers [content]; if it is taller or wider than the space (big fonts, long names, a notice), shrinks it to fit. */
@Composable
private fun FitToScreen(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, c ->
        val p = measurables.first().measure(c.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity))
        val scale = minOf(1f, c.maxHeight.toFloat() / p.height, c.maxWidth.toFloat() / p.width)
        layout(c.maxWidth, c.maxHeight) {
            // the layer scales around its own center, so centering the unscaled box centers the result
            p.placeWithLayer((c.maxWidth - p.width) / 2, (c.maxHeight - p.height) / 2) { scaleX = scale; scaleY = scale }
        }
    }
}

@Composable
private fun StatusPill(status: HomeStatus) {
    val ok = status == HomeStatus.READY
    val dot = if (ok) SystemGreen else Color(0xFFFF9F0A)
    val label = when (status) {
        HomeStatus.READY -> R.string.status_ready
        HomeStatus.OFF -> R.string.status_off
        HomeStatus.ERROR -> R.string.status_error
        HomeStatus.NEEDS_ACCESS -> R.string.local_network_label
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        // a soft halo makes the dot read as a light, not a sticker
        Box(Modifier.size(18.dp).background(dot.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(9.dp).background(dot, CircleShape))
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label), color = Label.copy(alpha = 0.85f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MacSteps(tvName: String) {
    val steps: List<Pair<Painter, String>> = listOf(
        painterResource(R.drawable.ic_control_center) to stringResource(R.string.home_step1),
        rememberVectorPainter(Icons.AutoMirrored.Filled.ScreenShare) to stringResource(R.string.home_step2),
        rememberVectorPainter(Icons.Default.Tv) to stringResource(R.string.home_step3, tvName),
    )
    BoxWithConstraints(Modifier.widthIn(max = 1120.dp)) {
        val wide = maxWidth >= 600.dp
        GlassPanel(Modifier.fillMaxWidth(if (wide) 0.86f else 1f)) {
            if (wide) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(IntrinsicSize.Max).padding(horizontal = 12.dp, vertical = 32.dp),
                ) {
                    steps.forEachIndexed { i, (icon, text) ->
                        if (i > 0) Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                            tint = Tertiary, modifier = Modifier.size(32.dp),
                        )
                        Step(i + 1, icon, text, last = i == steps.lastIndex, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    steps.forEachIndexed { i, (icon, text) -> Step(i + 1, icon, text, i == steps.lastIndex, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, icon: Painter, text: String, last: Boolean, modifier: Modifier) {
    Column(modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // the last step is the goal, so it gets the accent; the others stay quiet glass
        val tile = RoundedCornerShape(18.dp)
        Box(
            Modifier.size(72.dp).clip(tile).background(
                if (last) Brush.verticalGradient(listOf(Color(0xFF3B9BFF), SystemBlue))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f)))
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color.White)
        }
        Spacer(Modifier.height(18.dp))
        Text("$number", color = Tertiary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = Label, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
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
