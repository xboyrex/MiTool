package com.xboyrex.miunlocktool.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Calendar
import java.util.TimeZone

class SntpClient {

    var clockOffsetMs: Long = 0L
        private set

    var isSynchronized: Boolean = false
        private set

    companion object {
        private const val NTP_PORT = 123
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_MODE_CLIENT = 3
        private const val NTP_VERSION = 3
        private const val OFFSET_1900_TO_1970 = 2208988800L
        private const val TIMEOUT_MS = 5000

        val BEIJING_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

    suspend fun requestTime(host: String): Boolean = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            buffer[0] = ((NTP_MODE_CLIENT or (NTP_VERSION shl 3))).toByte()

            val requestTime = System.currentTimeMillis()
            writeTimeStamp(buffer, 40, requestTime)

            socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS

            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            val clientTransmitTimestamp = buffer.copyOfRange(40, 48)
            socket.connect(address, NTP_PORT)
            socket.send(requestPacket)

            val responsePacket = DatagramPacket(ByteArray(NTP_PACKET_SIZE), NTP_PACKET_SIZE)
            socket.receive(responsePacket)
            val responseTime = System.currentTimeMillis()

            if (responsePacket.length < NTP_PACKET_SIZE) return@withContext false

            val response = responsePacket.data
            val header = response[0].toInt() and 0xFF
            val leapIndicator = (header ushr 6) and 0x03
            val version = (header ushr 3) and 0x07
            val mode = header and 0x07
            val stratum = response[1].toInt() and 0xFF

            // Accept only valid server replies (LI != 3, NTP v3/v4, server mode, stratum 1..15).
            if (leapIndicator == 3 || version !in 3..4 || mode != 4 || stratum !in 1..15) {
                return@withContext false
            }

            // The server's originate timestamp must exactly echo our transmit timestamp.
            if (!response.copyOfRange(24, 32).contentEquals(clientTransmitTimestamp)) {
                return@withContext false
            }

            val t0 = readTimeStamp(response, 24)
            val t1 = readTimeStamp(response, 32)
            val t2 = readTimeStamp(response, 40)
            val t3 = responseTime

            if (t0 <= 0L || t1 <= 0L || t2 <= 0L) return@withContext false

            val offset = ((t1 - t0) + (t2 - t3)) / 2
            clockOffsetMs = offset
            isSynchronized = true
            true
        } catch (e: Exception) {
            false
        } finally {
            socket?.close()
        }
    }

    fun getSynchronizedTimeMs(): Long {
        return System.currentTimeMillis() + clockOffsetMs
    }

    fun getBeijingCalendar(): Calendar {
        val cal = Calendar.getInstance(BEIJING_TIMEZONE)
        cal.timeInMillis = getSynchronizedTimeMs()
        return cal
    }

    private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
        var seconds = time / 1000L
        val milliseconds = time - seconds * 1000L
        seconds += OFFSET_1900_TO_1970

        buffer[offset] = (seconds shr 24).toByte()
        buffer[offset + 1] = (seconds shr 16).toByte()
        buffer[offset + 2] = (seconds shr 8).toByte()
        buffer[offset + 3] = (seconds).toByte()

        val fraction = (milliseconds * 0x100000000L) / 1000L
        buffer[offset + 4] = (fraction shr 24).toByte()
        buffer[offset + 5] = (fraction shr 16).toByte()
        buffer[offset + 6] = (fraction shr 8).toByte()
        buffer[offset + 7] = (fraction).toByte()
    }

    private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
        val s0 = buffer[offset].toLong() and 0xFF
        val s1 = buffer[offset + 1].toLong() and 0xFF
        val s2 = buffer[offset + 2].toLong() and 0xFF
        val s3 = buffer[offset + 3].toLong() and 0xFF
        val seconds = (s0 shl 24) or (s1 shl 16) or (s2 shl 8) or s3

        val f0 = buffer[offset + 4].toLong() and 0xFF
        val f1 = buffer[offset + 5].toLong() and 0xFF
        val f2 = buffer[offset + 6].toLong() and 0xFF
        val f3 = buffer[offset + 7].toLong() and 0xFF
        val fraction = (f0 shl 24) or (f1 shl 16) or (f2 shl 8) or f3

        val ms = ((seconds - OFFSET_1900_TO_1970) * 1000L) + ((fraction * 1000L) / 0x100000000L)
        return ms
    }
}

