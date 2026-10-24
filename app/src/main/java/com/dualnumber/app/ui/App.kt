package com.dualnumber.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualnumber.app.data.Line
import com.dualnumber.app.util.Actions
import com.dualnumber.app.util.Setup

sealed interface Screen {
    data object Home : Screen
    data class Detail(val key: String, val line: Line) : Screen
    data object NewMessage : Screen
    data object Settings : Screen
}

@Composable
fun App(
    openRequest: Pair<String, Line>?,
    onOpenConsumed: () -> Unit,
    vm: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var onboarded by remember { mutableStateOf(vm.isOnboarded()) }

    fun needNotificationPermission() =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshSetup()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshSetup()
        if (!Setup.roleHeld(context)) {
            Actions.toast(context, "발신자 확인 앱으로 지정해야 전화를 구분할 수 있어요")
        }
        if (needNotificationPermission()) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** 1) 발신자 확인 앱 지정 → 2) 알림 허용 */
    val startSetup: () -> Unit = {
        val roleIntent = Setup.roleRequestIntent(context)
        when {
            !Setup.roleHeld(context) && roleIntent != null -> roleLauncher.launch(roleIntent)
            !Setup.roleHeld(context) -> Actions.openDefaultApps(context)
            needNotificationPermission() -> notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !Setup.notificationsEnabled(context) -> Actions.openNotificationSettings(context)
        }
    }

    LifecycleResumeEffect(Unit) {
        vm.refreshSetup()
        onPauseOrDispose { }
    }

    if (!onboarded) {
        OnboardingScreen(initial = state.config) { config ->
            vm.saveConfig(config)
            vm.setOnboarded()
            onboarded = true
            startSetup()
        }
        return
    }

    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val push: (Screen) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1, onBack = pop)
    val holder = rememberSaveableStateHolder()

    LaunchedEffect(openRequest) {
        val req = openRequest ?: return@LaunchedEffect
        stack = listOf(Screen.Home, Screen.Detail(req.first, req.second))
        onOpenConsumed()
    }

    AnimatedContent(
        targetState = stack.last(),
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 }) togetherWith fadeOut(tween(150))
        },
        label = "screen",
    ) { screen ->
        holder.SaveableStateProvider(screen.toString()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    state = state,
                    onOpen = { push(Screen.Detail(it.key, it.line)) },
                    onNew = { push(Screen.NewMessage) },
                    onSettings = { push(Screen.Settings) },
                    onSetup = startSetup,
                )
                is Screen.Detail -> DetailScreen(
                    state = state,
                    key = screen.key,
                    line = screen.line,
                    onRename = vm::setName,
                    onDelete = { vm.deleteConversation(screen.key, screen.line); pop() },
                    onBack = pop,
                )
                Screen.NewMessage -> NewMessageScreen(state, onSaveName = vm::setName, onBack = pop)
                Screen.Settings -> SettingsScreen(
                    state = state,
                    onSave = vm::saveConfig,
                    onSetup = startSetup,
                    onClearAll = vm::clearAll,
                    onBack = pop,
                )
            }
        }
    }
}
