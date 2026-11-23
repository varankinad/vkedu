package com.example.vkedu.domain.usecase

import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.repository.AppRepository
import javax.inject.Inject

class GetAppsUseCase @Inject constructor(
    private val appRepository: AppRepository,
) {
    suspend operator fun invoke(): Result<List<App>> = appRepository.getApps()
}
