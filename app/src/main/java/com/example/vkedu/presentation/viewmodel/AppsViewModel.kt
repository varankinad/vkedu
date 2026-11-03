package com.example.vkedu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.Result
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<Result<List<App>>>(Result.success(emptyList()))
    val apps = _apps.asStateFlow()

    private val _categories = MutableStateFlow<Result<List<Category>>>(Result.success(emptyList()))
    val categories = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val appsResult = appRepository.getApps()
            val categoriesResult = appRepository.getCategories()
            _apps.value = appsResult
            _categories.value = categoriesResult
            _isLoading.value = false
        }
    }

    fun getAppById(id: String): Result<App> {
        return _apps.value.getOrNull()?.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("App not found: $id"))
    }
}