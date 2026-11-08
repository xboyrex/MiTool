package com.xboyrex.miunlocktool.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PingTesterTest {
    @Test
    fun unavailableNetworkMeasurementsNeverUseSyntheticLatency() {
        val result = aggregatePingResults(emptyList(), emptyList())

        assertEquals(-1L, result.httpPingMs)
        assertEquals(-1L, result.tcpPingMs)
        assertEquals(-1L, result.averageMs)
    }

    @Test
    fun averageUsesOnlySuccessfullyMeasuredTransports() {
        val result = aggregatePingResults(listOf(100L, 120L), emptyList())

        assertEquals(110L, result.httpPingMs)
        assertEquals(-1L, result.tcpPingMs)
        assertEquals(110L, result.averageMs)
    }

    @Test
    fun averageCombinesBothAvailableTransportAverages() {
        val result = aggregatePingResults(listOf(100L, 120L), listOf(200L, 220L))

        assertEquals(110L, result.httpPingMs)
        assertEquals(210L, result.tcpPingMs)
        assertEquals(160L, result.averageMs)
    }
}
