package com.example.vkedu.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vkedu.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val appId: String? = savedStateHandle["appId"]
    private val _state = MutableStateFlow<AppDetailsState>(AppDetailsState.Loading)
    val state = _state.asStateFlow()

    private var installJob: Job? = null

    init {
        loadApp()
    }

    fun retry() {
        if (_state.value == AppDetailsState.Error) loadApp()
    }

    private fun loadApp() {
        val id = appId?.takeIf { it.isNotBlank() }
        if (id == null) {
            _state.value = AppDetailsState.Error
            return
        }

        _state.value = AppDetailsState.Loading
        viewModelScope.launch {
            try {
                _state.value = AppDetailsState.Content(appRepository.getAppById(id).getOrThrow())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AppDetailsState.Error
            }
        }
    }

    fun onInstallClick() {
        val content = _state.value as? AppDetailsState.Content ?: return
        if (content.installState is InstallState.Installing) {
            installJob?.cancel()
            installJob = null
            _state.update { state ->
                (state as? AppDetailsState.Content)?.copy(installState = InstallState.NotInstalled)
                    ?: state
            }
        } else {
            _state.update { state ->
                (state as? AppDetailsState.Content)?.copy(installState = InstallState.Installing(0))
                    ?: state
            }
            installJob = viewModelScope.launch { emulateInstall() }
        }
    }

    private suspend fun emulateInstall() {
        for (progress in 5..100 step 5) {
            delay(150)
            _state.update { state ->
                (state as? AppDetailsState.Content)?.copy(
                    installState = InstallState.Installing(progress),
                ) ?: state
            }
        }
        _state.update { state ->
            (state as? AppDetailsState.Content)?.copy(installState = InstallState.Installed) ?: state
        }
    }
}
