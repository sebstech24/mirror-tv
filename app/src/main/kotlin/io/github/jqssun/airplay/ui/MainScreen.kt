package io.github.jqssun.airplay.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn as AndroidxOptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.defaultServerName
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.service.AirPlayService.DisconnectReason
import io.github.jqssun.airplay.service.AirPlayService.ServerState
import io.github.jqssun.airplay.ui.gestures.BrightnessState
import io.github.jqssun.airplay.ui.gestures.DoubleTapIndicator
import io.github.jqssun.airplay.ui.gestures.GestureInfoText
import io.github.jqssun.airplay.ui.gestures.SeekGestureState
import io.github.jqssun.airplay.ui.gestures.TapGestureState
import io.github.jqssun.airplay.ui.gestures.VerticalGesture
import io.github.jqssun.airplay.ui.gestures.VideoContentScale
import io.github.jqssun.airplay.ui.gestures.VerticalProgressIndicator
import io.github.jqssun.airplay.ui.gestures.VideoPlayerGestures
import io.github.jqssun.airplay.ui.gestures.VolumeAndBrightnessGestureState
import io.github.jqssun.airplay.ui.gestures.ServerVolumeState
import io.github.jqssun.airplay.ui.gestures.ZoomState
import io.github.jqssun.airplay.viewmodel.DebugInfo
import io.github.jqssun.airplay.viewmodel.LocalNetworkAccess
import io.github.jqssun.airplay.viewmodel.MainViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.ui.compose.material3.MiniController
import androidx.media3.ui.compose.material3.buttons.NextButton
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.PreviousButton
import androidx.media3.ui.compose.material3.indicator.DurationText
import androidx.media3.ui.compose.material3.indicator.PositionText
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// Home + Settings only; About, license and logs hang off Settings
private enum class Screen(val titleRes: Int) {
    HOME(R.string.app_name),
    SETTINGS(R.string.tab_settings),
    ABOUT(R.string.setting_about),
    LICENSE(R.string.about_license_title),
    LOGS(R.string.tab_logs);

    val parent: Screen get() = when (this) {
        HOME, SETTINGS -> HOME
        ABOUT, LOGS -> SETTINGS
        LICENSE -> ABOUT
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isInPip: Boolean = false,
    onSurfaceAvailable: (android.view.Surface) -> Unit,
    onSurfaceDestroyed: (android.view.Surface) -> Unit,
    onPip: () -> Unit = {},
    onLocalNetwork: () -> Unit = {}
) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var fullscreen by remember { mutableStateOf(false) }
    val pin by viewModel.pinCode.collectAsState()
    val connections by viewModel.connectionCount.collectAsState()
    val audioOnly by viewModel.audioOnly.collectAsState()
    val videoPlaybackActive by viewModel.videoPlaybackActive.collectAsState()
    val videoSessionPending by viewModel.videoSessionPending.collectAsState()
    val mirroringActive by viewModel.mirroringActive.collectAsState()
    val autoFullscreen by viewModel.pref(Prefs.AUTO_FULLSCREEN).collectAsState()

    // don't use movableContentOf: moving AndroidView across subcomposition boundaries makes it crash on reparent
    val video: @Composable () -> Unit = {
        val aspect by viewModel.videoAspect.collectAsState()
        VideoSurfaceView(
            onSurfaceAvailable = onSurfaceAvailable,
            onSurfaceDestroyed = onSurfaceDestroyed,
            aspectRatio = aspect
        )
    }

    // fullscreen only once mirroring reports a size: connections rise before the kind is known
    var prevMirroringActive by remember { mutableStateOf(false) }
    LaunchedEffect(mirroringActive, audioOnly, videoPlaybackActive, pin) {
        val justStarted = !prevMirroringActive && mirroringActive
        prevMirroringActive = mirroringActive
        if (justStarted && !audioOnly && !videoPlaybackActive && autoFullscreen && pin == null) {
            fullscreen = true
        }
    }

    // manual fullscreen over the idle preview is deliberate; only undo on disconnect.
    // A dropped session shows a short "Mirroring stopped" notice instead of silently resetting.
    var prevConnections by remember { mutableIntStateOf(0) }
    var stoppedNotice by remember { mutableStateOf(false) }
    LaunchedEffect(connections) {
        val ended = prevConnections > 0 && connections == 0
        prevConnections = connections
        if (connections == 0) fullscreen = false
        if (connections > 0) {
            stoppedNotice = false
            screen = Screen.HOME
        }
        if (ended) {
            stoppedNotice = true
            delay(STOPPED_NOTICE_MS)
            stoppedNotice = false
        }
    }

    // leaving video playback must not fall through to a stale mirroring fullscreen
    LaunchedEffect(videoPlaybackActive) {
        if (videoPlaybackActive) fullscreen = false
    }

    val activity = LocalContext.current as? Activity
    val videoScreen = videoPlaybackActive || videoSessionPending
    LaunchedEffect(fullscreen, videoScreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen || videoScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (videoScreen) {
        val videoPlaybackAspect by viewModel.videoPlaybackAspect.collectAsState()
        val playback: @Composable () -> Unit = {
            VideoSurfaceView(
                onSurfaceAvailable = { viewModel.onVideoPlaybackSurfaceAvailable(it) },
                onSurfaceDestroyed = { viewModel.onVideoPlaybackSurfaceDestroyed(it) },
                aspectRatio = videoPlaybackAspect
            )
        }
        if (isInPip) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                playback()
            }
            return
        }
        val overlayTick by viewModel.videoOverlayTick.collectAsState()
        val playing by viewModel.videoPlaying.collectAsState()
        val positionMs by viewModel.videoPositionMs.collectAsState()
        val durationMs by viewModel.videoDurationMs.collectAsState()
        val scrubPositionMs by viewModel.videoScrubPositionMs.collectAsState()
        val scrubbing = scrubPositionMs != null
        var overlayVisible by remember { mutableStateOf(false) }
        LaunchedEffect(overlayTick, playing, scrubbing, videoPlaybackActive) {
            // tick 0 = fresh session; a pending session pins the overlay
            if (!videoPlaybackActive) {
                overlayVisible = true
            } else if (overlayTick == 0L) {
                overlayVisible = false
            } else {
                overlayVisible = true
                if (playing && !scrubbing) {
                    delay(VIDEO_OVERLAY_HIDE_MS)
                    overlayVisible = false
                }
            }
        }
        val gestureScope = rememberCoroutineScope()
        val tapGestureState = remember(viewModel) { TapGestureState(viewModel, gestureScope) }
        val seekGestureState = remember(viewModel) { SeekGestureState(viewModel) }
        val context = LocalContext.current
        val serverVolume = remember { ServerVolumeState(context) }
        val brightnessState = remember(activity) { activity?.window?.let { BrightnessState(it) } }
        val volumeAndBrightnessGestureState = remember(serverVolume, brightnessState) {
            VolumeAndBrightnessGestureState(serverVolume, brightnessState, gestureScope)
        }
        val zoomState = remember(viewModel) { ZoomState(viewModel, gestureScope) }
        var controlsLocked by remember { mutableStateOf(false) }
        DisposableEffect(serverVolume) { serverVolume.handleLifecycle(this) }
        LaunchedEffect(overlayTick) {
            if (overlayTick == 0L) {
                zoomState.reset()
                controlsLocked = false
            }
        }
        DisposableEffect(brightnessState) {
            onDispose { brightnessState?.clearOverride() }
        }
        // orientation follows the video aspect; manual rotate holds until the next video
        LaunchedEffect(videoPlaybackAspect) {
            activity?.requestedOrientation = if (videoPlaybackAspect < 1f) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
        DisposableEffect(activity) {
            onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
        val videoPlaybackSize by viewModel.videoPlaybackSize.collectAsState()
        val buffering by viewModel.videoBuffering.collectAsState()
        val videoTitle by viewModel.videoTitle.collectAsState()
        val videoLocation by viewModel.videoLocation.collectAsState()
        val speed by viewModel.videoSpeed.collectAsState()
        val isPipSupported = remember {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }
        var showSpeedSelector by remember { mutableStateOf(false) }
        BackHandler { viewModel.stopVideoPlayback() }

        val rootFocusRequester = remember { FocusRequester() }
        val playPauseFocusRequester = remember { FocusRequester() }
        val unlockFocusRequester = remember { FocusRequester() }
        var isPlayPauseFocused by remember { mutableStateOf(false) }
        var isUnlockFocused by remember { mutableStateOf(false) }
        LaunchedEffect(overlayVisible, controlsLocked, showSpeedSelector) {
            if (showSpeedSelector) return@LaunchedEffect
            if (!overlayVisible) {
                runCatching { rootFocusRequester.requestFocus() }
                return@LaunchedEffect
            }
            val locked = controlsLocked
            val target = if (locked) unlockFocusRequester else playPauseFocusRequester
            target.requestFocusUntilLanded(attempts = 20) { if (locked) isUnlockFocused else isPlayPauseFocused }
        }

        // dpad seeking (controls hidden): accumulate the skipped amount and briefly show it
        var dpadSeekOffsetMs by remember { mutableLongStateOf(0L) }
        var dpadSeekTargetMs by remember { mutableLongStateOf(0L) }
        var dpadSeekActive by remember { mutableStateOf(false) }
        var dpadSeekTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(dpadSeekTick) {
            if (!dpadSeekActive) return@LaunchedEffect
            delay(1000)
            dpadSeekActive = false
        }
        val showDpadSeekFeedback: (Long) -> Unit = { deltaMs ->
            if (!dpadSeekActive) dpadSeekOffsetMs = 0L
            dpadSeekOffsetMs += deltaMs
            dpadSeekTargetMs = (dpadSeekTargetMs.takeIf { dpadSeekActive } ?: positionMs).plus(deltaMs)
                .coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
            dpadSeekActive = true
            dpadSeekTick++
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(rootFocusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (showSpeedSelector) {
                        false
                    } else {
                        handlePlayerKeyEvent(
                            keyEvent = keyEvent,
                            controlsVisible = overlayVisible,
                            controlsLocked = controlsLocked,
                            isPlayPauseFocused = isPlayPauseFocused,
                            seekIncrementMs = TapGestureState.SEEK_INCREMENT_MS,
                            viewModel = viewModel,
                            showControls = { viewModel.showVideoOverlay() },
                            unlockControls = {
                                controlsLocked = false
                                viewModel.showVideoOverlay()
                            },
                            onDpadSeek = showDpadSeekFeedback,
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            VideoContentFrame(
                aspect = videoPlaybackAspect,
                videoSizePx = videoPlaybackSize,
                contentScale = zoomState.contentScale,
                zoom = zoomState.zoom
            ) { sizeModifier ->
                VideoSurfaceView(
                    onSurfaceAvailable = { viewModel.onVideoPlaybackSurfaceAvailable(it) },
                    onSurfaceDestroyed = { viewModel.onVideoPlaybackSurfaceDestroyed(it) },
                    applyAspectRatio = false,
                    modifier = sizeModifier
                )
            }
            VideoPlayerGestures(
                enabled = videoPlaybackActive,
                locked = controlsLocked,
                onTap = {
                    // a pending session pins the overlay; taps must not unpin it
                    if (!videoPlaybackActive) return@VideoPlayerGestures
                    if (overlayVisible) overlayVisible = false else viewModel.showVideoOverlay()
                },
                tapGestureState = tapGestureState,
                seekGestureState = seekGestureState,
                volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                zoomState = zoomState
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = overlayVisible && videoPlaybackActive && !controlsLocked,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }
            if (buffering) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(72.dp))
            }
            DoubleTapIndicator(tapGestureState = tapGestureState)
            val seekAmountMs = seekGestureState.seekAmountMs
            when {
                seekAmountMs != null -> GestureInfoText(
                    info = "${if (seekAmountMs < 0) "-" else "+"}${formatVideoTime(abs(seekAmountMs))}\n" +
                        "[${formatVideoTime(seekGestureState.targetPositionMs ?: 0)}]",
                    modifier = Modifier.align(Alignment.Center)
                )
                zoomState.isZooming -> GestureInfoText(
                    info = "${(zoomState.zoom * 100).toInt()}%",
                    modifier = Modifier.align(Alignment.Center)
                )
                zoomState.showContentScaleIndicator -> GestureInfoText(
                    info = stringResource(zoomState.contentScale.nameRes()),
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> androidx.compose.animation.AnimatedVisibility(
                    visible = overlayVisible && videoPlaybackActive && !controlsLocked,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    PlayPauseButton(
                        playing = playing,
                        onClick = { viewModel.toggleVideoPlayPause() },
                        modifier = Modifier
                            .focusRequester(playPauseFocusRequester)
                            .onFocusChanged { isPlayPauseFocused = it.hasFocus }
                    )
                }
            }
            DpadSeekIndicator(
                visible = dpadSeekActive && dpadSeekOffsetMs != 0L,
                offsetMs = dpadSeekOffsetMs,
                positionMs = dpadSeekTargetMs
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.VOLUME,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(24.dp)
            ) {
                VerticalProgressIndicator(value = serverVolume.percentage, icon = Icons.AutoMirrored.Rounded.VolumeUp)
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.BRIGHTNESS,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(24.dp)
            ) {
                VerticalProgressIndicator(value = brightnessState?.percentage ?: 0, icon = Icons.Rounded.BrightnessHigh)
            }
            if (controlsLocked) {
                if (overlayVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(top = 24.dp)
                    ) {
                        UnlockButton(
                            onClick = {
                                controlsLocked = false
                                viewModel.showVideoOverlay()
                            },
                            modifier = Modifier
                                .focusRequester(unlockFocusRequester)
                                .onFocusChanged { isUnlockFocused = it.hasFocus }
                        )
                    }
                }
            } else {
                androidx.compose.animation.AnimatedVisibility(
                    visible = overlayVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    VideoControlsTop(
                        title = videoTitle,
                        videoUrl = videoLocation,
                        onBackClick = { viewModel.stopVideoPlayback() },
                        onSpeedClick = {
                            overlayVisible = false
                            showSpeedSelector = true
                        }
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = overlayVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    VideoControlsBottom(
                        positionMs = scrubPositionMs ?: positionMs,
                        durationMs = durationMs,
                        contentScale = zoomState.contentScale,
                        isPipSupported = isPipSupported,
                        onRotateClick = {
                            activity?.let {
                                it.requestedOrientation =
                                    if (it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                            }
                        },
                        onLockClick = {
                            viewModel.showVideoOverlay()
                            controlsLocked = true
                        },
                        onContentScaleClick = {
                            viewModel.showVideoOverlay()
                            zoomState.switchToNextContentScale()
                        },
                        onPipClick = onPip,
                        onSeek = { seekGestureState.onSeek(it) },
                        onSeekEnd = { seekGestureState.onSeekEnd() },
                        seekBarModifier = Modifier
                            .focusProperties { up = playPauseFocusRequester }
                            .dpadAdjust(
                                onLeft = {
                                    viewModel.seekVideoBy(-TapGestureState.SEEK_INCREMENT_MS)
                                    viewModel.showVideoOverlay()
                                },
                                onRight = {
                                    viewModel.seekVideoBy(TapGestureState.SEEK_INCREMENT_MS)
                                    viewModel.showVideoOverlay()
                                }
                            )
                    )
                }
            }
            val skipSilence by viewModel.videoSkipSilence.collectAsState()
            PlaybackSpeedSelector(
                show = showSpeedSelector,
                speed = speed,
                skipSilence = skipSilence,
                onSpeedChange = { viewModel.setVideoSpeed(it) },
                onSkipSilenceChange = { viewModel.setVideoSkipSilence(it) },
                onDismiss = { showSpeedSelector = false }
            )
        }
        return
    }

    // pip mode: show only the video surface
    if (isInPip) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            video()
        }
        return
    }

    // restore system bars when exiting pip back to non-fullscreen
    LaunchedEffect(isInPip) {
        if (!isInPip && !fullscreen) {
            val window = activity?.window ?: return@LaunchedEffect
            WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // exit fullscreen while a pin is being shown so the dialog isn't covered
    LaunchedEffect(pin) {
        if (pin != null) fullscreen = false
    }

    // back from Settings while a Mac is still mirroring goes straight back to the full-screen picture
    LaunchedEffect(screen, mirroringActive) {
        if (screen == Screen.HOME && mirroringActive && autoFullscreen && !audioOnly && !videoPlaybackActive && pin == null) {
            fullscreen = true
        }
    }

    if (fullscreen) {
        FullscreenVideo(
            viewModel = viewModel,
            video = video,
            onSettings = { fullscreen = false; screen = Screen.SETTINGS },
            onPip = onPip
        )
    } else if (screen == Screen.HOME) {
        HomeContent(
            viewModel, video,
            onFullscreen = { fullscreen = true }, onPip = onPip, onLocalNetwork = onLocalNetwork,
            onSettings = { screen = Screen.SETTINGS },
            showAudioMode = audioOnly,
            stoppedNotice = stoppedNotice
        )
    } else {
        BackHandler { screen = screen.parent }
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
                ) {
                    IconButton(onClick = { screen = screen.parent }, modifier = Modifier.dpadFocus(CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(screen.titleRes), style = MaterialTheme.typography.headlineMedium)
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (screen) {
                        Screen.SETTINGS -> SettingsScreen(
                            viewModel,
                            onOpenAbout = { screen = Screen.ABOUT },
                            onOpenLogs = { screen = Screen.LOGS }
                        )
                        Screen.ABOUT -> AboutScreen(onViewLicense = { screen = Screen.LICENSE })
                        Screen.LICENSE -> LicenseScreen()
                        Screen.LOGS -> LogsScreen(viewModel)
                        Screen.HOME -> Unit
                    }
                }
                AudioMiniController(
                    viewModel,
                    visible = audioOnly && connections > 0,
                    onClick = { screen = Screen.HOME }
                )
            }
        }
    }

    // pin dialog
    if (pin != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPin() },
            title = { Text(stringResource(R.string.dialog_pin_title)) },
            text = {
                Text(
                    text = pin!!,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPin() }) { Text(stringResource(R.string.btn_ok)) }
            }
        )
    }

}

@Composable
private fun HomeContent(
    viewModel: MainViewModel,
    video: @Composable () -> Unit,
    onFullscreen: () -> Unit,
    onPip: () -> Unit,
    onLocalNetwork: () -> Unit,
    onSettings: () -> Unit,
    showAudioMode: Boolean,
    stoppedNotice: Boolean
) {
    val state by viewModel.serverState.collectAsState()
    val localNetwork by viewModel.localNetwork.collectAsState()
    val running = state == ServerState.RUNNING
    val clientName by viewModel.clientName.collectAsState()
    val lastDisconnect by viewModel.lastDisconnect.collectAsState()
    val grant = !running && localNetwork != LocalNetworkAccess.GRANTED
    val connections by viewModel.connectionCount.collectAsState()
    val serverName by viewModel.pref(Prefs.SERVER_NAME).collectAsState()
    val videoResolution by viewModel.videoResolution.collectAsState()
    val idlePreview by viewModel.pref(Prefs.IDLE_PREVIEW).collectAsState()
    val mirroringActive by viewModel.mirroringActive.collectAsState()
    val debugEnabled by viewModel.pref(Prefs.DEBUG_ENABLED).collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()
    val context = LocalContext.current
    val address = remember(state) { context.tvIpv4() }
    // blank pref = the TV's own device name
    val deviceName = remember { context.defaultServerName() }
    val advertisedName by viewModel.advertisedName.collectAsState()
    val tvName = advertisedName.takeIf { running && it.isNotBlank() } ?: serverName.ifBlank { deviceName }

    val status = when {
        grant -> HomeStatus.NEEDS_ACCESS
        running -> HomeStatus.READY
        state == ServerState.ERROR -> HomeStatus.ERROR
        else -> HomeStatus.OFF
    }
    // the only button on the home screen ever turns things ON; switching off lives in Settings → Advanced
    val actionLabel = when (status) {
        HomeStatus.NEEDS_ACCESS -> stringResource(
            if (localNetwork == LocalNetworkAccess.SETTINGS) R.string.btn_open_settings else R.string.btn_allow
        )
        HomeStatus.OFF -> stringResource(R.string.btn_turn_on)
        HomeStatus.ERROR -> stringResource(R.string.btn_try_again)
        HomeStatus.READY -> null
    }
    val onAction = if (grant) onLocalNetwork else viewModel::startServer

    val tv = isTv()
    val gearFocus = remember { FocusRequester() }
    val actionFocus = remember { FocusRequester() }
    val audioMode = showAudioMode && running && connections > 0
    val waiting = !audioMode && !(running && connections > 0)
    LaunchedEffect(tv, actionLabel != null, waiting) {
        if (tv) (if (waiting && actionLabel != null) actionFocus else gearFocus).requestFocusUntilLanded()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!audioMode && running && (mirroringActive || idlePreview)) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { video() }
        }
        when {
            audioMode -> Box(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 48.dp, vertical = 24.dp)) {
                NowPlayingContent(viewModel)
            }
            running && mirroringActive -> Unit
            running && connections > 0 -> ConnectingContent(clientName, takingOver = lastDisconnect == DisconnectReason.REPLACED)
            else -> WaitingContent(
                tvName = tvName,
                status = status,
                address = address,
                // read live: the reason may land a poll after the connection count drops
                noticeTitle = if (!stoppedNotice) null else stringResource(when (lastDisconnect) {
                    DisconnectReason.SENDER_STOPPED -> R.string.home_stopped_mac_title
                    DisconnectReason.CONNECTION_LOST -> R.string.home_lost_title
                    DisconnectReason.NETWORK_CHANGED -> R.string.home_network_title
                    else -> R.string.home_stopped_title
                }),
                noticeText = if (!stoppedNotice) null else stringResource(when (lastDisconnect) {
                    DisconnectReason.SENDER_STOPPED -> R.string.home_stopped_mac_desc
                    DisconnectReason.CONNECTION_LOST -> R.string.home_lost_desc
                    DisconnectReason.NETWORK_CHANGED -> R.string.home_network_desc
                    else -> R.string.home_stopped_desc
                }, tvName),
                actionLabel = actionLabel,
                onAction = onAction,
                actionFocus = actionFocus,
                modifier = Modifier.safeDrawingPadding()
            )
        }

        if (debugEnabled && connections > 0) {
            DebugOverlay(debugInfo, Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp))
        }
        var showRes by remember { mutableStateOf(false) }
        LaunchedEffect(videoResolution) {
            if (videoResolution.isNotEmpty() && !showAudioMode) {
                showRes = true
                delay(5000)
                showRes = false
            }
        }
        if (!showAudioMode) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showRes && connections > 0,
                modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(8.dp)
            ) {
                Text(
                    text = videoResolution,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(horizontal = 32.dp, vertical = 16.dp)) {
            val tint = if (mirroringActive) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            if (running && mirroringActive && !audioMode) {
                IconButton(onClick = onPip, modifier = Modifier.dpadFocus(CircleShape)) {
                    Icon(painterResource(R.drawable.ic_pip), contentDescription = stringResource(R.string.cd_pip), tint = tint)
                }
                IconButton(onClick = onFullscreen, modifier = Modifier.dpadFocus(CircleShape)) {
                    Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.cd_fullscreen), tint = tint)
                }
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(56.dp).focusRequester(gearFocus).dpadFocus(CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings), tint = tint, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun FullscreenVideo(
    viewModel: MainViewModel,
    video: @Composable () -> Unit,
    onSettings: () -> Unit,
    onPip: () -> Unit
) {
    val videoResolution by viewModel.videoResolution.collectAsState()
    val debugEnabled by viewModel.pref(Prefs.DEBUG_ENABLED).collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()

    // OK and Back both open the same control bar; Back again (or 10 s without a key) hides it
    var controlsVisible by remember { mutableStateOf(false) }
    var tapTick by remember { mutableStateOf(0) }
    LaunchedEffect(tapTick, controlsVisible) {
        if (controlsVisible) { delay(CONTROLS_HIDE_MS); controlsVisible = false }
    }
    BackHandler { if (controlsVisible) controlsVisible = false else { controlsVisible = true; tapTick++ } }
    val rootFocus = remember { FocusRequester() }
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) firstButton.requestFocusUntilLanded() else rootFocus.requestFocusUntilLanded()
    }

    val clientName by viewModel.clientName.collectAsState()
    val lastDisconnect by viewModel.lastDisconnect.collectAsState()
    var toastVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(MIRRORING_TOAST_MS)
        toastVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // any key keeps the bar up; OK/Enter opens it
                tapTick++
                if (!controlsVisible && e.key in OPEN_KEYS) { controlsVisible = true; true } else false
            }
            .focusable()
            .pointerInput(Unit) { detectTapGestures { controlsVisible = true; tapTick++ } },
        contentAlignment = Alignment.Center
    ) {
        video()
        androidx.compose.animation.AnimatedVisibility(
            visible = toastVisible && !controlsVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
        ) {
            Text(
                text = when {
                    clientName == null -> stringResource(R.string.toast_mirroring)
                    lastDisconnect == DisconnectReason.REPLACED -> stringResource(R.string.toast_took_over, clientName!!)
                    else -> stringResource(R.string.toast_mirroring_from, clientName!!)
                } + "  ·  " + stringResource(R.string.controls_hint),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MirrorControlBar(
                clientName = clientName,
                resolution = videoResolution,
                firstButton = firstButton,
                onSettings = onSettings,
                onPip = onPip,
                onHide = { controlsVisible = false },
            )
        }
        if (debugEnabled) {
            DebugOverlay(debugInfo, Modifier.align(Alignment.TopStart).padding(8.dp))
        }
    }
}

/** The bar OK/Back opens while mirroring: who is mirroring, the picture size, and big labelled buttons. */
@Composable
private fun MirrorControlBar(
    clientName: String?,
    resolution: String,
    firstButton: FocusRequester,
    onSettings: () -> Unit,
    onPip: () -> Unit,
    onHide: () -> Unit,
) {
    val ctx = LocalContext.current
    val canPip = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xF0181B22),
        contentColor = Color.White,
        tonalElevation = 6.dp,
        modifier = Modifier
            .safeDrawingPadding()
            // overscan-safe, and wide enough to read from the couch
            .padding(start = 60.dp, end = 60.dp, bottom = 36.dp)
            .widthIn(max = 1100.dp)
            .fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 28.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.ic_launcher_art), contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(13.dp))
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                )
                Text(
                    clientName?.let { stringResource(R.string.toast_mirroring_from, it) } ?: stringResource(R.string.toast_mirroring),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(resolution.ifBlank { null }, stringResource(R.string.controls_back_hides)).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(16.dp))
            BarButton(Icons.Default.Settings, stringResource(R.string.cd_settings), onSettings, Modifier.focusRequester(firstButton))
            if (canPip) {
                Spacer(Modifier.width(12.dp))
                BarButton(Icons.Default.PictureInPictureAlt, stringResource(R.string.cd_pip), onPip)
            }
            Spacer(Modifier.width(12.dp))
            BarButton(Icons.Default.KeyboardArrowDown, stringResource(R.string.controls_hide), onHide)
        }
    }
}

@Composable
private fun BarButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Button(
        onClick = onClick,
        interactionSource = source,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
            contentColor = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier.dpadFocus(RoundedCornerShape(16.dp))
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

// dacp scanning is press-and-hold: beginff/beginrew while held, playresume on release
@Composable
private fun HoldScanButton(
    icon: ImageVector,
    contentDescription: String,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onBegin()
                    try {
                        awaitRelease()
                    } finally {
                        onEnd()
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription)
    }
}

@Composable
@AndroidxOptIn(UnstableApi::class)
private fun AudioMiniController(viewModel: MainViewModel, visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    val player = viewModel.dacpPlayer ?: return
    val context = LocalContext.current
    MiniController(
        player,
        bitmapLoader = remember { DataSourceBitmapLoader.Builder(context).build() },
        onClick = onClick
    )
}

@Composable
@AndroidxOptIn(UnstableApi::class)
private fun NowPlayingContent(viewModel: MainViewModel) {
    val track by viewModel.trackInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // cover art
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (track.coverArt != null) {
                Image(
                    bitmap = track.coverArt!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_cover_art),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // track info
        Text(
            text = track.title.ifEmpty { stringResource(R.string.unknown_track) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (track.artist.isNotEmpty()) {
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (track.album.isNotEmpty()) {
            Text(
                text = track.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(16.dp))

        val player = viewModel.dacpPlayer
        if (player != null) {
            // BottomControls' default row, minus its video-overlay gradient
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PositionText(player, Modifier.padding(end = 8.dp))
                Box(modifier = Modifier.weight(1f)) { ProgressSlider(player) }
                DurationText(player, Modifier.padding(start = 8.dp))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HoldScanButton(
                    icon = Icons.Default.FastRewind,
                    contentDescription = stringResource(R.string.cd_rewind),
                    onBegin = { viewModel.audioScanBegin(false) },
                    onEnd = { viewModel.audioScanEnd() }
                )
                PreviousButton(player, modifier = Modifier.dpadFocus())
                PlayPauseButton(
                    player,
                    modifier = Modifier.size(63.dp).dpadFocus(CircleShape),
                    iconSize = 40.dp
                )
                NextButton(player, modifier = Modifier.dpadFocus())
                HoldScanButton(
                    icon = Icons.Default.FastForward,
                    contentDescription = stringResource(R.string.cd_fast_forward),
                    onBegin = { viewModel.audioScanBegin(true) },
                    onEnd = { viewModel.audioScanEnd() }
                )
            }

            Spacer(Modifier.height(8.dp))

            val context = LocalContext.current
            val serverVolume = remember { ServerVolumeState(context) }
            DisposableEffect(serverVolume) { serverVolume.handleLifecycle(this) }
            VolumeSlider(
                icon = painterResource(R.drawable.ic_download),
                label = stringResource(R.string.volume_server),
                value = serverVolume.percentage / 100f,
                onValueChange = { serverVolume.update((it * 100).roundToInt()) },
                onStep = { serverVolume.adjust(it > 0) }
            )

            val clientVolume by viewModel.clientVolume.collectAsState()
            val clientAdjustable by viewModel.clientVolumeAdjustable.collectAsState()
            // sender reports back only after each dacp step
            var clientDrag by remember { mutableStateOf<Float?>(null) }
            val clientShown = clientDrag ?: clientVolume
            VolumeSlider(
                icon = painterResource(R.drawable.ic_upload),
                label = stringResource(R.string.volume_client),
                value = clientShown,
                enabled = clientAdjustable,
                onValueChange = { clientDrag = it; viewModel.setClientVolume(it) },
                onValueChangeFinished = { clientDrag = null },
                onStep = { viewModel.setClientVolume((clientShown + it * CLIENT_VOLUME_STEP).coerceIn(0f, 1f)) }
            )
        }
    }
}

@Composable
private fun VolumeSlider(
    icon: Painter,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onStep: (Int) -> Unit,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier
                    .semantics { contentDescription = label }
                    .dpadFocus()
                    .dpadAdjust(onLeft = { onStep(-1) }, onRight = { onStep(1) })
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.volume_value, (value * 100).roundToInt()),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 40.dp)
        )
    }
}

// sender has 16 volume steps above mute
private const val CLIENT_VOLUME_STEP = 1f / 16

private const val VIDEO_OVERLAY_HIDE_MS = 4000L
private const val STOPPED_NOTICE_MS = 8000L
private const val MIRRORING_TOAST_MS = 4000L
private const val CONTROLS_HIDE_MS = 10_000L
// remote keys that open the control bar while mirroring
private val OPEN_KEYS = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Menu)

@Composable
private fun DebugOverlay(info: DebugInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val style = MaterialTheme.typography.labelSmall
        val headingStyle = style.copy(fontWeight = FontWeight.Bold)
        val color = Color.White.copy(alpha = 0.9f)
        val headingColor = Color(0xFF80D8FF) // cyan
        val context = LocalContext.current
        debugOverlaySections(context, info).forEachIndexed { i, section ->
            if (i > 0) Spacer(Modifier.height(6.dp))
            Text(section.title.uppercase(), style = headingStyle, color = headingColor)
            section.lines.forEach { Text(it, style = style, color = color) }
        }
    }
}

private data class DebugSection(val title: String, val lines: List<String>)

private fun debugOverlaySections(context: Context, info: DebugInfo): List<DebugSection> = buildList {
    buildList {
        if (info.videoCodec.isNotEmpty()) {
            add(context.getString(R.string.debug_video, info.videoCodec, info.videoRes))
            add(context.getString(R.string.debug_fps_bitrate, info.videoFps, info.bitrateStr))
            add(context.getString(R.string.debug_frames_drops, info.videoFrames, info.droppedFrames))
            add(context.getString(R.string.debug_jitter, info.jitterStr))
        }
    }.takeIf { it.isNotEmpty() }?.let { add(DebugSection(context.getString(R.string.debug_section_video), it)) }

    buildList {
        if (info.audioCodec.isNotEmpty()) add(context.getString(R.string.debug_audio, info.audioCodec, info.serverVolume, info.clientVolume))
        info.audio?.let { a ->
            add(context.getString(R.string.debug_audio_buffer, a.backlogMs, a.tunedCushionMs))
            add(context.getString(R.string.debug_audio_glitch, a.trims, a.drops, a.silences, a.underruns, a.xrun))
            add(context.getString(R.string.debug_audio_decode, formatDecode(a.decodeMeanUs, a.decodeMaxUs, a.decodeHeld, a.decodeErrors)))
        }
    }.takeIf { it.isNotEmpty() }?.let { add(DebugSection(context.getString(R.string.debug_section_audio), it)) }

    add(DebugSection(context.getString(R.string.debug_section_network), listOf(context.getString(R.string.debug_clients, info.connections))))
}

// "mean/max ms held=N", or "held=N" before first latency window lands
private fun formatDecode(meanUs: Int, maxUs: Int, held: Int, errors: Int): String =
    (if (meanUs == 0) "held=$held"
    else "%.1f/%.1f ms held=%d".format(meanUs / 1000.0, maxUs / 1000.0, held)) + " errs=$errors"
