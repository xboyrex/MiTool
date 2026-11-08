package com.xboyrex.miunlocktool.unlock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MiUnlockCryptoTest {
    @Test
    fun decodesValidHexBytes() {
        assertArrayEquals(byteArrayOf(0x00, 0x7F, 0xFF.toByte()), MiUnlockCrypto.hexToBytes("007fff"))
    }

    @Test
    fun rejectsOddLengthHex() {
        assertThrows(IllegalArgumentException::class.java) {
            MiUnlockCrypto.hexToBytes("abc")
        }
    }

    @Test
    fun rejectsNonHexCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            MiUnlockCrypto.hexToBytes("00xz")
        }
    }

    @Test
    fun rejectsEmptyToken() {
        assertThrows(IllegalArgumentException::class.java) {
            MiUnlockCrypto.hexToBytes("  ")
        }
    }
}
