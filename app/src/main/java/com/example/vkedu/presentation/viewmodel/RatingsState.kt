package com.example.vkedu.presentation.viewmodel

data class RatingsState(
    val ratings: Map<String, Float> = emptyMap(),
    val loadingMode: RatingsLoadingMode? = null,
    val idCount: Int = 0,
    val sequentialTimeMillis: Long? = null,
    val parallelTimeMillis: Long? = null,
    val hasError: Boolean = false,
)

enum class RatingsLoadingMode {
    Sequential,
    Parallel,
}
