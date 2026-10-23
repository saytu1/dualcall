package com.dualnumber.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dualnumber.app.data.CommRepository
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Event
import com.dualnumber.app.data.Prefs
import com.dualnumber.app.data.REQUIRED_PERMISSIONS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val loading: Boolean = true,
    val events: List<Event> = emptyList(),
    val contacts: Map<String, String> = emptyMap(),
    val granted: Set<String> = emptySet(),
    val config: Config = Config(),
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val repo = CommRepository(app)
    private val _state = MutableStateFlow(UiState(config = prefs.load()))
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var job: Job? = null

    fun isOnboarded(): Boolean = prefs.onboarded
    fun setOnboarded() {
        prefs.onboarded = true
    }

    /** 통화기록·문자·연락처를 다시 읽는다. 화면에 돌아올 때마다 호출. */
    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val granted = REQUIRED_PERMISSIONS.filter { repo.has(it) }.toSet()
                val contacts = repo.loadContacts()
                val events = (repo.loadCalls() + repo.loadSms()).sortedByDescending { it.time }
                Triple(granted, contacts, events)
            }
            _state.update {
                it.copy(loading = false, granted = result.first, contacts = result.second, events = result.third)
            }
        }
    }

    fun saveConfig(config: Config) {
        prefs.save(config)
        _state.update { it.copy(config = config) }
    }
}
