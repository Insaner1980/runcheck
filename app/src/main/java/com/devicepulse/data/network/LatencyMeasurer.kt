package com.devicepulse.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatencyMeasurer @Inject constructor() {

    suspend fun measureLatency(endpoint: String = DEFAULT_ENDPOINT): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS

                val startTime = System.nanoTime()
                connection.connect()
                connection.responseCode
                val endTime = System.nanoTime()

                connection.disconnect()
                ((endTime - startTime) / 1_000_000).toInt()
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        private const val DEFAULT_ENDPOINT = "https://www.google.com"
        private const val TIMEOUT_MS = 5000
    }
}
