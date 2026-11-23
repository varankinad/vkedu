package com.example.vkedu.presentation.viewmodel

import com.example.vkedu.domain.model.App

sealed interface AppDetailsState {
    data object Loading : AppDetailsState
    data object Error : AppDetailsState
    data class Content(
        val app: App,
        val installState: InstallState = InstallState.NotInstalled,
    ) : AppDetailsState
}

sealed interface InstallState {
    data object NotInstalled : InstallState
    data class Installing(val progress: Int) : InstallState
    data object Installed : InstallState
}
