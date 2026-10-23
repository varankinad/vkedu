package com.example.vkedu.presentation.appslist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AppsListViewModel(
    appsProvider: () -> List<App> = ::loadApps,
) : ViewModel() {
    val uiState: StateFlow<AppsListUiState> =
        MutableStateFlow(AppsListUiState(apps = appsProvider())).asStateFlow()

    // Transient clicks must not replay when the screen starts collecting again.
    private val _snackbarEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val snackbarEvents: SharedFlow<Unit> = _snackbarEvents.asSharedFlow()

    fun onToolbarClick() {
        _snackbarEvents.tryEmit(Unit)
    }
}

data class AppsListUiState(val apps: List<App>)
