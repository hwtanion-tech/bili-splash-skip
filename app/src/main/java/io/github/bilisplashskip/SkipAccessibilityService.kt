package io.github.bilisplashskip

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

class SkipAccessibilityService : AccessibilityService() {
    private val gate = SessionGate()
    private lateinit var settings: AppSettings

    override fun onServiceConnected() {
        settings = AppSettings(this)
        gate.reset()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::settings.isInitialized) return
        if (!settings.enabled) {
            gate.reset()
            return
        }
        val root = rootInActiveWindow ?: return
        try {
            // Observe the actual active window, not an event from a background app.
            val packageName = root.packageName?.toString()
            val now = SystemClock.elapsedRealtime()
            gate.observe(packageName, now)
            if (!SkipPolicy.isTarget(packageName) || !gate.canScan(now)) return
            if (clickSkip(root)) {
                gate.markClicked()
                settings.recordClick()
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun clickSkip(root: AccessibilityNodeInfo): Boolean {
        val windowBounds = Rect()
        root.getBoundsInScreen(windowBounds)
        if (windowBounds.isEmpty) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        for (i in 0 until root.childCount) root.getChild(i)?.let(queue::addLast)
        var visited = 0
        try {
            while (queue.isNotEmpty() && visited++ < 500) {
                val node = queue.removeFirst()
                try {
                    if (isCandidate(node, windowBounds) && clickCandidate(node, windowBounds)) return true
                    for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
                } finally {
                    @Suppress("DEPRECATION")
                    node.recycle()
                }
            }
        } finally {
            while (queue.isNotEmpty()) {
                @Suppress("DEPRECATION")
                queue.removeFirst().recycle()
            }
        }
        return false
    }

    private fun isCandidate(node: AccessibilityNodeInfo, window: Rect): Boolean {
        if (!SkipPolicy.isTarget(node.packageName?.toString())) return false
        if (!node.isVisibleToUser || !node.isEnabled) return false
        if (!SkipPolicy.isSkipLabel(node.text?.toString()) &&
            !SkipPolicy.isSkipLabel(node.contentDescription?.toString())) return false
        return hasSafeBounds(node, window)
    }

    private fun hasSafeBounds(node: AccessibilityNodeInfo, window: Rect): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (window.isEmpty || bounds.isEmpty || !window.contains(bounds)) return false
        return SkipPolicy.isSafeButtonBounds(
            (bounds.left - window.left).toDouble() / window.width(),
            (bounds.top - window.top).toDouble() / window.height(),
            (bounds.right - window.left).toDouble() / window.width(),
            (bounds.bottom - window.top).toDouble() / window.height(),
        )
    }

    private fun clickCandidate(node: AccessibilityNodeInfo, window: Rect): Boolean {
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val labelBounds = Rect()
        node.getBoundsInScreen(labelBounds)
        // TextViews sometimes delegate clicking to a small enclosing button. Never
        // ascend beyond two levels, or click an ad-sized / unrelated container.
        var parent = node.parent
        try {
            repeat(2) {
                val current = parent ?: return false
                if (!SkipPolicy.isTarget(current.packageName?.toString()) ||
                    !current.isVisibleToUser || !current.isEnabled ||
                    !hasSafeBounds(current, window)) return false
                val parentBounds = Rect()
                current.getBoundsInScreen(parentBounds)
                if (!parentBounds.contains(labelBounds)) return false
                if (current.isClickable) return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent = current.parent
                @Suppress("DEPRECATION")
                current.recycle()
            }
        } finally {
            @Suppress("DEPRECATION")
            parent?.recycle()
        }
        return false
    }

    override fun onInterrupt() { gate.reset() }
}
