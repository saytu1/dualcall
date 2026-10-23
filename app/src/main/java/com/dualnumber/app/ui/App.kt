package com.dualnumber.app.ui

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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.REQUIRED_PERMISSIONS

sealed interface Screen {
    data object Home : Screen
    data class Detail(val key: String, val line: Line) : Screen
    data object NewMessage : Screen
    data object Settings : Screen
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var onboarded by remember { mutableStateOf(vm.isOnboarded()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh() }
    val requestPermissions: () -> Unit = { permissionLauncher.launch(REQUIRED_PERMISSIONS) }

    // 앱으로 돌아올 때마다 새 기록 반영
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    if (!onboarded) {
        OnboardingScreen(initial = state.config) { config ->
            vm.saveConfig(config)
            vm.setOnboarded()
            onboarded = true
            requestPermissions()
        }
        return
    }

    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val push: (Screen) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1, onBack = pop)
    val holder = rememberSaveableStateHolder()

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
                onRequestPermissions = requestPermissions,
            )
            is Screen.Detail -> DetailScreen(state, screen.key, screen.line, onBack = pop)
            Screen.NewMessage -> NewMessageScreen(state, onBack = pop)
            Screen.Settings -> SettingsScreen(
                state = state,
                onSave = vm::saveConfig,
                onBack = pop,
                onRequestPermissions = requestPermissions,
            )
        }
        }
    }
}
