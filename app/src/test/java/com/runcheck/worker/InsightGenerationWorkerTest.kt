package com.runcheck.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.insights.engine.InsightEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightGenerationWorkerTest {
    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val insightEngine: InsightEngine = mockk()

    @Test
    fun `doWork returns success when insight generation succeeds`() =
        runTest {
            coEvery { insightEngine.generateInsights(any()) } returns Unit

            val worker =
                InsightGenerationWorker(
                    appContext = context,
                    workerParams = workerParams,
                    insightEngine = insightEngine,
                )

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            coVerify(exactly = 1) { insightEngine.generateInsights(any()) }
        }

    @Test
    fun `doWork returns retry when insight generation throws`() =
        runTest {
            coEvery { insightEngine.generateInsights(any()) } throws IllegalStateException("boom")

            val worker =
                InsightGenerationWorker(
                    appContext = context,
                    workerParams = workerParams,
                    insightEngine = insightEngine,
                )

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Retry)
            coVerify(exactly = 1) { insightEngine.generateInsights(any()) }
        }
}
