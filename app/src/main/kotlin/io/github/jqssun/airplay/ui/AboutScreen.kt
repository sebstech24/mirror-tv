package io.github.jqssun.airplay.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.R
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(onViewLicense: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val url = stringResource(R.string.about_source_url)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusUntilLanded() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.about_credits), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.about_license_note), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.about_ffmpeg), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.about_source, url), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onViewLicense, modifier = Modifier.focusRequester(first).dpadFocus(CircleShape)) {
                Text(stringResource(R.string.about_view_license))
            }
            // many TVs have no browser; the URL stays readable on screen either way
            OutlinedButton(
                onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                modifier = Modifier.dpadFocus(CircleShape),
            ) { Text(url) }
        }
        Text(
            stringResource(R.string.about_trademark),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Full GPL-3.0 text from res/raw; D-pad up/down scrolls a page at a time. */
@Composable
fun LicenseScreen() {
    val ctx = LocalContext.current
    val text = remember {
        runCatching { ctx.resources.openRawResource(R.raw.license_gpl3).bufferedReader().use { it.readText() } }.getOrDefault("")
    }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusUntilLanded() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = when (e.key) {
                    Key.DirectionDown -> 400f
                    Key.DirectionUp -> -400f
                    else -> return@onPreviewKeyEvent false
                }
                scope.launch { scroll.animateScrollBy(step) }
                true
            }
            .verticalScroll(scroll)
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}
