package com.example.vkedu.domain.usecase

import com.example.vkedu.domain.repository.AppRepository
import javax.inject.Inject

class LoadRatingsSequentiallyUseCase @Inject constructor(
    private val appRepository: AppRepository,
) {
    suspend operator fun invoke(ids: List<String>): Map<String, Float> =
        appRepository.loadRatingsSequentially(ids)
}
