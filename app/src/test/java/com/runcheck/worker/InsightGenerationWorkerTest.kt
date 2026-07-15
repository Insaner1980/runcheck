package com.runcheck.worker

import android.content.Context
import android.database.SQLException
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.insights.engine.InsightEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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
    fun `doWork returns retry when insight generation throws database exception`() =
        runTest {
            coEvery { insightEngine.generateInsights(any()) } throws SQLException("boom")

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

    @Test
    fun `doWork returns failure when insight generation throws non-database exception`() =
        runTest {
            coEvery { insightEngine.generateInsights(any()) } throws IllegalStateException("boom")

            val worker =
                InsightGenerationWorker(
                    appContext = context,
                    workerParams = workerParams,
                    insightEngine = insightEngine,
                )

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            coVerify(exactly = 1) { insightEngine.generateInsights(any()) }
        }

    @Test
    fun `doWork rethrows cancellation`() =
        runTest {
            coEvery { insightEngine.generateInsights(any()) } throws CancellationException("stopped")
            val worker =
                InsightGenerationWorker(
                    appContext = context,
                    workerParams = workerParams,
                    insightEngine = insightEngine,
                )

            val thrown = runCatching { worker.doWork() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
        }
}
