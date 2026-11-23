package com.example.vkedu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.usecase.GetAppsUseCase
import com.example.vkedu.domain.usecase.LoadRatingsInParallelUseCase
import com.example.vkedu.domain.usecase.LoadRatingsSequentiallyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val getAppsUseCase: GetAppsUseCase,
    private val loadRatingsSequentiallyUseCase: LoadRatingsSequentiallyUseCase,
    private val loadRatingsInParallelUseCase: LoadRatingsInParallelUseCase,
) : ViewModel() {

    private val _apps = MutableStateFlow<Result<List<App>>>(Result.success(emptyList()))
    val apps = _apps.asStateFlow()

    private val _categories = MutableStateFlow<Result<List<Category>>>(Result.success(emptyList()))
    val categories = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _ratingsState = MutableStateFlow(RatingsState())
    val ratingsState = _ratingsState.asStateFlow()
    private var comparisonJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        if (_isLoading.value) return

        comparisonJob?.cancel()
        comparisonJob = null
        _ratingsState.value = RatingsState()
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val appsResult = getAppsUseCase()
                _apps.value = appsResult
                _categories.value = appsResult.map { apps ->
                    apps.map { it.category }.distinctBy { it.key }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun compareRatings() {
        if (_isLoading.value || _ratingsState.value.loadingMode != null) return
        val apps = _apps.value.getOrNull() ?: return
        val ids = apps.map { it.id }.distinct()
        if (ids.isEmpty()) return

        _ratingsState.update { state ->
            state.copy(
                loadingMode = RatingsLoadingMode.Sequential,
                idCount = ids.size,
                sequentialTimeMillis = null,
                parallelTimeMillis = null,
                hasError = false,
            )
        }

        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val currentJob = coroutineContext.job
            try {
                lateinit var sequentialRatings: Map<String, Float>
                val sequentialTime = measureTimeMillis {
                    sequentialRatings = loadRatingsSequentiallyUseCase(ids)
                }
                coroutineContext.ensureActive()
                _ratingsState.update { state ->
                    state.copy(
                        ratings = sequentialRatings,
                        sequentialTimeMillis = sequentialTime,
                        loadingMode = RatingsLoadingMode.Parallel,
                    )
                }

                lateinit var parallelRatings: Map<String, Float>
                val parallelTime = measureTimeMillis {
                    parallelRatings = loadRatingsInParallelUseCase(ids)
                }
                coroutineContext.ensureActive()
                _ratingsState.update { state ->
                    state.copy(ratings = parallelRatings, parallelTimeMillis = parallelTime)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _ratingsState.update { state -> state.copy(hasError = true) }
            } finally {
                if (comparisonJob === currentJob) {
                    comparisonJob = null
                    _ratingsState.update { state -> state.copy(loadingMode = null) }
                }
            }
        }
        comparisonJob = job
        job.start()
    }

    fun getAppById(id: String): Result<App> {
        return _apps.value.getOrNull()?.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("App not found: $id"))
    }
}
