package com.xboyrex.miunlocktool.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class PingResult(
    val httpPingMs: Long,
    val tcpPingMs: Long,
    val averageMs: Long
)

class PingTester {

    private val client = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .build()

    private val httpHosts = listOf(
        "https://sgp-api.buy.mi.com",
        "https://api.buy.mi.com",
        "https://account.xiaomi.com"
    )

    private val tcpHosts = listOf(
        Pair("sgp-api.buy.mi.com", 443),
        Pair("api.buy.mi.com", 443),
        Pair("account.xiaomi.com", 443)
    )

    fun httpPing(url: String, timeoutMs: Int = 1500): Long? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) XiaomiUnlockTool")
                .head()
                .build()
            val start = System.currentTimeMillis()
            client.newBuilder()
                .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { response ->
                    val duration = System.currentTimeMillis() - start
                    if (response.code < 500) duration else null
                }
        } catch (e: Exception) {
            null
        }
    }

    fun tcpPing(host: String, port: Int = 443, timeoutMs: Int = 1500): Long? {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            null
        }
    }

    suspend fun measurePings(durationSeconds: Int = 15): PingResult = withContext(Dispatchers.IO) {
        val httpPings = mutableListOf<Long>()
        val tcpPings = mutableListOf<Long>()
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)

        while (System.currentTimeMillis() < endTime) {
            for (url in httpHosts) {
                httpPing(url)?.let { httpPings.add(it) }
            }
            for ((host, port) in tcpHosts) {
                tcpPing(host, port)?.let { tcpPings.add(it) }
            }
            delay(500)
        }

        aggregatePingResults(httpPings, tcpPings)
    }

    suspend fun measureQuickPing(): PingResult = withContext(Dispatchers.IO) {
        val httpPings = mutableListOf<Long>()
        val tcpPings = mutableListOf<Long>()

        for (url in httpHosts) {
            httpPing(url)?.let { httpPings.add(it) }
        }
        for ((host, port) in tcpHosts) {
            tcpPing(host, port)?.let { tcpPings.add(it) }
        }

        aggregatePingResults(httpPings, tcpPings)
    }

    suspend fun measureAveragePingMs(count: Int = 1): Long {
        val samples = mutableListOf<Long>()
        repeat(count.coerceIn(1, 10)) {
            val sample = measureQuickPing().averageMs
            if (sample >= 0L) samples += sample
            if (it < count.coerceIn(1, 10) - 1) delay(150)
        }
        return if (samples.isEmpty()) -1L else samples.average().toLong()
    }
}

internal fun aggregatePingResults(
    httpPings: List<Long>,
    tcpPings: List<Long>
): PingResult {
    val httpAverage = httpPings.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: -1L
    val tcpAverage = tcpPings.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: -1L
    val available = listOf(httpAverage, tcpAverage).filter { it >= 0L }
    val combined = if (available.isEmpty()) -1L else available.average().toLong()
    return PingResult(
        httpPingMs = httpAverage,
        tcpPingMs = tcpAverage,
        averageMs = combined
    )
}
