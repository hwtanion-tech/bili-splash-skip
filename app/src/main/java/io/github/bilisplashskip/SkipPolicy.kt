package io.github.bilisplashskip

/** Conservative, pure rules: never match a general sentence containing 跳过. */
object SkipPolicy {
    const val TARGET_PACKAGE = "tv.danmaku.bili"
    const val SESSION_MS = 8_000L
    const val SCAN_INTERVAL_MS = 200L

    private val skipLabel = Regex(
        "^(?:跳过(?:广告)?(?:[（(]?\\d{1,2}(?:s|秒)?[）)]?)?|(?:[（(]?\\d{1,2}(?:s|秒)?[）)]?)跳过(?:广告)?)$",
        RegexOption.IGNORE_CASE,
    )

    fun isTarget(packageName: String?) = packageName == TARGET_PACKAGE

    // Normalized bounds, relative to the active application window.
    fun isSafeButtonBounds(left: Double, top: Double, right: Double, bottom: Double): Boolean {
        if (!listOf(left, top, right, bottom).all { it.isFinite() }) return false
        if (left < 0 || top < 0 || right > 1 || bottom > 1) return false
        if (right <= left || bottom <= top) return false
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        return centerX >= 0.55 && (centerY <= 0.30 || centerY >= 0.75) &&
            right - left <= 0.40 && bottom - top <= 0.15
    }

    fun isSkipLabel(label: String?): Boolean {
        if (label == null || label.length > 30) return false
        val compact = label.replace(Regex("\\s+"), "")
        return skipLabel.matches(compact)
    }
}

/** A new target-app foreground session allows at most one successful click. */
class SessionGate {
    private var foreground: String? = null
    private var startedAt = 0L
    private var lastScan = Long.MIN_VALUE
    private var completed = false

    fun observe(packageName: String?, now: Long) {
        if (packageName == null || packageName == foreground) return
        foreground = packageName
        if (SkipPolicy.isTarget(packageName)) {
            startedAt = now
            lastScan = Long.MIN_VALUE
            completed = false
        }
    }

    fun canScan(now: Long): Boolean {
        if (!SkipPolicy.isTarget(foreground) || completed) return false
        if (now < startedAt || now - startedAt >= SkipPolicy.SESSION_MS) return false
        if (lastScan != Long.MIN_VALUE && now - lastScan < SkipPolicy.SCAN_INTERVAL_MS) return false
        lastScan = now
        return true
    }

    fun markClicked() { completed = true }

    fun reset() {
        foreground = null
        completed = false
        lastScan = Long.MIN_VALUE
    }
}
