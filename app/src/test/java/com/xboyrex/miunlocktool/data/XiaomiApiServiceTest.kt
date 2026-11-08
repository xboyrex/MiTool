package com.xboyrex.miunlocktool.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XiaomiApiServiceTest {
    private val service = XiaomiApiService()

    @Test
    fun freeFormNegativeTextMustNotBecomeSuccessfulResponse() {
        val parsed = service.parseApiResponse("Request not approved; try again later")

        assertNull(parsed.code)
        assertNull(parsed.applyResult)
    }

    @Test
    fun freeFormSuccessWordMustNotBecomeSuccessfulResponse() {
        val parsed = service.parseApiResponse("The server says this request was successful")

        assertNull(parsed.code)
        assertNull(parsed.applyResult)
    }

    @Test
    fun structuredSuccessfulApplyResponseRetainsExplicitResult() {
        val parsed = service.parseApiResponse(
            """{"code":0,"data":{"apply_result":1}}"""
        )

        assertEquals(0, parsed.code)
        assertEquals(1, parsed.applyResult)
    }

    @Test
    fun quotaQueuedCodeRemainsExplicitlyRecognized() {
        val parsed = service.parseApiResponse("""{"code":100003}""")

        assertEquals(100003, parsed.code)
    }
}
