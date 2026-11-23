package com.example.vkedu.domain.usecase

import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.repository.AppRepository
import javax.inject.Inject

class GetAppByIdUseCase @Inject constructor(
    private val appRepository: AppRepository,
) {
    suspend operator fun invoke(id: String): Result<App> = appRepository.getAppById(id)
}
