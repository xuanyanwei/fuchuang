package com.aurora.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aurora.player.data.model.PlaylistItem
import com.aurora.player.data.repository.PlaylistRepository
import com.aurora.player.player.AuroraPlayerController
import com.aurora.player.ui.screens.HistoryScreen
import com.aurora.player.ui.screens.HomeScreen
import com.aurora.player.ui.screens.PlaylistScreen
import com.aurora.player.ui.screens.SettingsScreen
import com.aurora.player.ui.theme.AuroraCyan
import com.aurora.player.ui.theme.AuroraDarkBackground
import com.aurora.player.ui.theme.AuroraDarkSurface
import com.aurora.player.ui.theme.AuroraPlayerTheme
import com.aurora.player.ui.theme.AuroraTextPrimary
import com.aurora.player.ui.theme.AuroraTextSecondary

enum class AppTab(val title: String) {
    HOME("播放"),
    PLAYLIST("列表"),
    HISTORY("历史"),
    SETTINGS("设置")
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: PlaylistRepository
    private lateinit var playerController: AuroraPlayerController

    private var isFullscreenMode by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Support display cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        repository = PlaylistRepository(this)
        playerController = AuroraPlayerController(
            context = this,
            onPositionSaved = { item, pos, dur ->
                repository.recordHistory(item, pos, dur)
            }
        )

        // Auto play first demo item if available
        val initialItems = repository.playlistItems.value
        if (initialItems.isNotEmpty()) {
            playerController.play(initialItems.first())
        }

        setContent {
            AuroraPlayerTheme {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        var currentTab by remember { mutableStateOf(AppTab.HOME) }
        val playerState by playerController.playerState.collectAsState()
        val allItems by repository.playlistItems.collectAsState()
        val historyList by repository.history.collectAsState()
        val headerConfig by repository.headerConfig.collectAsState()
        val groupedPlaylist = remember(allItems) { repository.getGroupedPlaylist() }

        // Back Handler for fullscreen
        BackHandler(enabled = isFullscreenMode) {
            setFullscreen(false)
        }

        Scaffold(
            bottomBar = {
                AnimatedVisibility(visible = !isFullscreenMode && !isInPipMode) {
                    NavigationBar(
                        containerColor = AuroraDarkSurface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.HOME,
                            onClick = { currentTab = AppTab.HOME },
                            icon = { Icon(Icons.Default.PlayCircle, contentDescription = "播放") },
                            label = { Text("播放", fontSize = 11.sp) },
                            colors = navColors(),
                            modifier = Modifier.testTag("nav_tab_home")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.PLAYLIST,
                            onClick = { currentTab = AppTab.PLAYLIST },
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "列表") },
                            label = { Text("列表", fontSize = 11.sp) },
                            colors = navColors(),
                            modifier = Modifier.testTag("nav_tab_playlist")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.HISTORY,
                            onClick = { currentTab = AppTab.HISTORY },
                            icon = { Icon(Icons.Default.History, contentDescription = "历史") },
                            label = { Text("历史", fontSize = 11.sp) },
                            colors = navColors(),
                            modifier = Modifier.testTag("nav_tab_history")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.SETTINGS,
                            onClick = { currentTab = AppTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                            label = { Text("设置", fontSize = 11.sp) },
                            colors = navColors(),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            },
            containerColor = AuroraDarkBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreenMode || isInPipMode) androidx.compose.foundation.layout.PaddingValues(0.dp) else paddingValues)
            ) {
                when (currentTab) {
                    AppTab.HOME -> {
                        HomeScreen(
                            controller = playerController,
                            playerState = playerState,
                            groupedPlaylist = groupedPlaylist,
                            allPlaylistItems = allItems,
                            isFullscreen = isFullscreenMode,
                            onToggleFullscreen = { toggleFullscreen() },
                            onEnterPip = { enterPip() },
                            onPlayItem = { item ->
                                playerController.play(item)
                            },
                            onAddSingleStream = { title, url ->
                                val newItem = repository.addSingleStream(title, url)
                                playerController.play(newItem)
                            },
                            onOpenPlaylistTab = { currentTab = AppTab.PLAYLIST }
                        )
                    }
                    AppTab.PLAYLIST -> {
                        PlaylistScreen(
                            groups = groupedPlaylist,
                            currentPlayingItem = playerState.currentItem,
                            onPlayItem = { item ->
                                playerController.play(item)
                                currentTab = AppTab.HOME
                            },
                            onToggleFavorite = { url ->
                                repository.toggleFavorite(url)
                            },
                            onImportUrl = { url ->
                                repository.fetchRemotePlaylist(url)
                            },
                            onImportText = { text ->
                                repository.importLocalM3uContent(text)
                            }
                        )
                    }
                    AppTab.HISTORY -> {
                        HistoryScreen(
                            historyList = historyList,
                            onResumePlay = { historyItem ->
                                val playlistItem = PlaylistItem(
                                    id = historyItem.id,
                                    title = historyItem.title,
                                    url = historyItem.url,
                                    groupTitle = historyItem.groupTitle,
                                    headers = historyItem.headers
                                )
                                playerController.play(playlistItem, startPositionMs = historyItem.positionMs)
                                currentTab = AppTab.HOME
                            },
                            onClearHistory = {
                                repository.clearHistory()
                            }
                        )
                    }
                    AppTab.SETTINGS -> {
                        SettingsScreen(
                            headerConfig = headerConfig,
                            onSaveHeaderConfig = { newConfig ->
                                repository.updateHeaderConfig(newConfig)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun navColors() = NavigationBarItemDefaults.colors(
        selectedIconColor = AuroraDarkBackground,
        selectedTextColor = AuroraCyan,
        indicatorColor = AuroraCyan,
        unselectedIconColor = AuroraTextSecondary,
        unselectedTextColor = AuroraTextSecondary
    )

    private fun setFullscreen(enable: Boolean) {
        isFullscreenMode = enable
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun toggleFullscreen() {
        setFullscreen(!isFullscreenMode)
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        playerController.saveCurrentPosition()
        if (!isInPipMode) {
            playerController.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController.release()
    }
}
