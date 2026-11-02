package io.github.bilisplashskip

import org.junit.Assert.*
import org.junit.Test

class SkipPolicyTest {
    @Test fun acceptsOnlyShortSkipLabels() {
        listOf("跳过", "跳过 4", "跳过广告", "跳过 5s", "5 秒 跳过", "跳过（3秒）", "跳过广告(5S)")
            .forEach { assertTrue(it, SkipPolicy.isSkipLabel(it)) }
        listOf(null, "", "点击跳过这个视频", "跳过片头", "广告", "跳过并购买", "skip", "跳过100秒")
            .forEach { assertFalse(it, SkipPolicy.isSkipLabel(it)) }
    }

    @Test fun filtersPackages() {
        assertTrue(SkipPolicy.isTarget("tv.danmaku.bili"))
        assertFalse(SkipPolicy.isTarget("com.bilibili.app.in"))
        assertFalse(SkipPolicy.isTarget(null))
    }

    @Test fun acceptsScreenshotBottomRightButton() {
        assertTrue(SkipPolicy.isSafeButtonBounds(617.0 / 864, 1747.0 / 1920, 831.0 / 864, 1855.0 / 1920))
        assertTrue(SkipPolicy.isSafeButtonBounds(0.75, 0.05, 0.95, 0.10))
    }

    @Test fun rejectsLargeCentralLeftOrInvalidContainers() {
        assertFalse(SkipPolicy.isSafeButtonBounds(0.0, 0.0, 1.0, 1.0))
        assertFalse(SkipPolicy.isSafeButtonBounds(0.7, 0.45, 0.9, 0.50))
        assertFalse(SkipPolicy.isSafeButtonBounds(0.05, 0.85, 0.25, 0.90))
        assertFalse(SkipPolicy.isSafeButtonBounds(0.5, 0.8, 1.0, 0.9))
        assertFalse(SkipPolicy.isSafeButtonBounds(0.7, 0.7, 0.9, 0.95))
        assertFalse(SkipPolicy.isSafeButtonBounds(0.7, 0.9, 1.1, 0.95))
        assertFalse(SkipPolicy.isSafeButtonBounds(0.9, 0.9, 0.8, 0.95))
        assertFalse(SkipPolicy.isSafeButtonBounds(Double.NaN, 0.9, 0.9, 0.95))
    }

    @Test fun sessionExpiresEvenWithContinuousEvents() {
        val gate = SessionGate()
        gate.observe(SkipPolicy.TARGET_PACKAGE, 100L)
        assertTrue(gate.canScan(100L))
        assertFalse(gate.canScan(200L))
        assertTrue(gate.canScan(300L))
        gate.observe(SkipPolicy.TARGET_PACKAGE, 8100L)
        assertFalse(gate.canScan(8100L))
    }

    @Test fun successBlocksFurtherClicksUntilNewForegroundSession() {
        val gate = SessionGate()
        gate.observe(SkipPolicy.TARGET_PACKAGE, 0L)
        assertTrue(gate.canScan(0L))
        gate.markClicked()
        assertFalse(gate.canScan(500L))
        gate.observe("com.android.launcher", 600L)
        assertFalse(gate.canScan(600L))
        gate.observe(SkipPolicy.TARGET_PACKAGE, 700L)
        assertTrue(gate.canScan(700L))
    }

    @Test fun resetDisablesSession() {
        val gate = SessionGate()
        gate.observe(SkipPolicy.TARGET_PACKAGE, 0L)
        gate.reset()
        assertFalse(gate.canScan(500L))
    }
}
