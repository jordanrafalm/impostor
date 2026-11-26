package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateTest {
    @Test
    fun `versions use numeric components without overflow`() {
        assertTrue(compareNumericVersions("1.10", "1.9.9")!! > 0)
        assertEquals(0, compareNumericVersions("01.002.0", "1.2"))
        assertTrue(compareNumericVersions("999999999999999999999", "99")!! > 0)
        for (invalid in listOf("", ".1", "1.", "1..2", "1-beta", " 1", "١", "+1")) {
            assertNull(compareNumericVersions(invalid, "1"))
            assertNull(compareNumericVersions("1", invalid))
        }
    }

    @Test
    fun `only newer compatible valid listings qualify`() {
        assertTrue(isNewerCompatibleListing("1.2", "1.3", "17.2", null))
        assertTrue(isNewerCompatibleListing("1.2", "1.3", "17.2", "17.2.0"))
        assertFalse(isNewerCompatibleListing("1.2", "1.2.0", "17.2", null))
        assertFalse(isNewerCompatibleListing("1.3", "1.2", "17.2", null))
        assertFalse(isNewerCompatibleListing("1.2", "1.3", "17.2", "18"))
        assertFalse(isNewerCompatibleListing("1.2", "1.3", "17.2", "invalid"))
        assertFalse(isNewerCompatibleListing("1.2", "1.3beta", "17.2", null))
        assertFalse(isNewerCompatibleListing("invalid", "1.3", "17.2", null))
    }

    @Test
    fun `update does not interrupt another prompt or gameplay`() {
        assertTrue(mayOfferUpdate(true, true, false, false))
        assertFalse(mayOfferUpdate(false, true, false, false))
        assertFalse(mayOfferUpdate(true, false, false, false))
        assertFalse(mayOfferUpdate(true, true, true, false))
        assertFalse(mayOfferUpdate(true, true, false, true))
    }
}
