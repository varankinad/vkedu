package com.example.vkedu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vkedu.data.repository.AppRepositoryImpl
import com.example.vkedu.domain.repository.AppRepository

class AppsViewModelFactory : ViewModelProvider.Factory {
    private val appRepository: AppRepository = AppRepositoryImpl()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppsViewModel(appRepository) as T
    }
}