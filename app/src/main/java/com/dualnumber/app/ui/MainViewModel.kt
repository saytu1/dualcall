package com.dualnumber.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Event
import com.dualnumber.app.data.EventStore
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.NameStore
import com.dualnumber.app.data.Prefs
import com.dualnumber.app.service.Notifier
import com.dualnumber.app.util.Setup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SetupStatus(
    val roleAvailable: Boolean = true,
    val roleHeld: Boolean = false,
    val notificationsOk: Boolean = true,
)

data class UiState(
    val events: List<Event> = emptyList(),
    val names: Map<String, String> = emptyMap(),
    val config: Config = Config(),
    val setup: SetupStatus = SetupStatus(),
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val config = MutableStateFlow(prefs.load())
    private val setup = MutableStateFlow(readSetup())

    init {
        EventStore.init(app)
        NameStore.init(app)
        Notifier.ensureChannels(app)
    }

    val state: StateFlow<UiState> =
        combine(EventStore.events, NameStore.names, config, setup) { e, n, c, s -> UiState(e, n, c, s) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                UiState(EventStore.events.value, NameStore.names.value, config.value, setup.value),
            )

    private fun readSetup() = SetupStatus(
        roleAvailable = Setup.roleAvailable(app),
        roleHeld = Setup.roleHeld(app),
        notificationsOk = Setup.notificationsEnabled(app),
    )

    fun refreshSetup() {
        setup.value = readSetup()
    }

    fun isOnboarded(): Boolean = prefs.onboarded
    fun setOnboarded() {
        prefs.onboarded = true
    }

    fun saveConfig(c: Config) {
        prefs.save(c)
        config.value = c
    }

    fun setName(key: String, name: String?) = NameStore.set(app, key, name)
    fun deleteConversation(key: String, line: Line) = EventStore.deleteConversation(app, key, line)
    fun clearAll() = EventStore.clear(app)
}
