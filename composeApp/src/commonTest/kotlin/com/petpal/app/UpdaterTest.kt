package com.petpal.app

import com.petpal.app.domain.platform.AppUpdater
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdaterTest {
    @Test
    fun versionCompare() {
        assertTrue(AppUpdater.isNewer("v0.15.0", "0.14.0"))
        assertTrue(AppUpdater.isNewer("0.14.1", "0.14"))
        assertTrue(AppUpdater.isNewer("1.0.0", "0.99.9"))
        assertFalse(AppUpdater.isNewer("v0.14.0", "0.14.0"))
        assertFalse(AppUpdater.isNewer("0.13.9", "0.14.0"))
    }
}
