package com.example.vkedu.data.repository

import com.example.vkedu.data.remote.api.CatalogApi
import com.example.vkedu.data.remote.dto.AppDetailDto
import com.example.vkedu.data.remote.dto.CatalogAppDto
import com.example.vkedu.data.source.AppsDataSource
import java.util.Locale
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class RatingsBenchmarkTest {
    @Test
    fun benchmarkThreeRealSequentialThenParallelLoads() {
        assumeTrue(
            "The real ratings benchmark requires RUN_RATINGS_BENCHMARK=1",
            System.getenv("RUN_RATINGS_BENCHMARK") == "1",
        )

        val ids = AppsDataSource.apps.map { it.id }.distinct()
        assertEquals("AppsDataSource must provide 14 unique benchmark IDs", 14, ids.size)
        val repository = AppRepositoryImpl(object : CatalogApi {
            override suspend fun getCatalog(): List<CatalogAppDto> =
                error("The benchmark must not fetch the HTTP catalog")

            override suspend fun getAppDetail(id: String): AppDetailDto =
                error("The benchmark must not fetch HTTP app details")
        })

        println("Ratings benchmark source: AppsDataSource.apps; unique ID count=${ids.size}")
        println(
            "Ratings benchmark runtime: Java ${System.getProperty("java.version")} " +
                "(${System.getProperty("java.vendor")}); ${System.getProperty("os.name")} " +
                "${System.getProperty("os.version")} ${System.getProperty("os.arch")}",
        )
        runBlocking {
            repeat(3) { run ->
                val sequentialMillis = measureTimeMillis {
                    repository.loadRatingsSequentially(ids)
                }
                val parallelMillis = measureTimeMillis {
                    repository.loadRatingsInParallel(ids)
                }
                val ratio = String.format(Locale.ROOT, "%.3f", sequentialMillis.toDouble() / parallelMillis)
                println(
                    "Ratings benchmark run ${run + 1}: sequential=$sequentialMillis ms, " +
                        "parallel=$parallelMillis ms, sequential/parallel=$ratio",
                )
            }
        }
    }
}
