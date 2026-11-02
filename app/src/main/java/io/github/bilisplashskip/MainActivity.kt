package io.github.bilisplashskip

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var settings: AppSettings
    private lateinit var status: TextView
    private lateinit var history: TextView
    private lateinit var toggle: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }

        content.addView(label("Bili 开屏助手", 28))
        content.addView(label("少一次手动点击，多一点清净。", 16))
        status = label("", 18)
        content.addView(status)
        toggle = Switch(this).apply {
            text = "启用自动跳过"
            textSize = 18f
            setPadding(0, dp(16), 0, dp(16))
            isChecked = settings.enabled
            setOnCheckedChangeListener { _, checked ->
                if (checked && !settings.enabled) {
                    isChecked = false
                    showConsent()
                } else if (!checked) {
                    settings.enabled = false
                    refresh()
                }
            }
        }
        content.addView(toggle)
        content.addView(button("打开系统无障碍设置") {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: android.content.ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })
        content.addView(label("使用方法", 20))
        content.addView(label("1. 开启上方开关并阅读授权说明。\n2. 在系统无障碍设置中启用“Bili 开屏助手”。\n3. 从其他应用切换或重新打开哔哩哔哩。", 16))
        content.addView(label("工作范围与限制", 20))
        content.addView(label("仅适配国内官方版哔哩哔哩（tv.danmaku.bili）。进入前台后的 8 秒内，只尝试点击右上或右下区域中可识别的“跳过”控件及其小尺寸可点击父控件，每次最多成功一次。\n\n不会删除广告，也不能保证广告不展示。无按钮、按钮无法访问或位置不符时不会点击；不使用坐标点击、截图或联网。通知栏等其他窗口可能使一次检测会话重新开始。", 16))
        history = label("", 16)
        content.addView(label("本地记录", 20))
        content.addView(history)
        content.addView(button("清空运行记录") {
            settings.clearHistory()
            refresh()
        })
        content.addView(button("关闭并停用无障碍服务") {
            settings.enabled = false
            toggle.isChecked = false
            // The user controls service authorization in Android settings.
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(label("隐私：不申请网络、存储或悬浮窗权限，不保存界面内容。仅保存开关、点击成功次数和最后一次时间。与哔哩哔哩官方无关联。", 14))
    }

    private fun showConsent() {
        AlertDialog.Builder(this)
            .setTitle("无障碍权限用途说明")
            .setMessage("此功能需要你手动授权无障碍服务，以读取前台界面节点，并在哔哩哔哩开屏时点击符合规则的“跳过”按钮。\n\n为判断前台切换，服务会收到其他应用的窗口事件，但不会遍历其界面或保存内容。识别可能失败或误判；你可随时关闭开关或在系统设置中停用服务。所有数据仅保存在本机。\n\n是否同意并启用功能？")
            .setNegativeButton("取消", null)
            .setPositiveButton("同意并启用") { _, _ ->
                settings.enabled = true
                toggle.isChecked = true
                refresh()
            }.show()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (!::status.isInitialized) return
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val component = ComponentName(this, SkipAccessibilityService::class.java)
        val authorized = manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        ).any {
            val info = it.resolveInfo.serviceInfo
            ComponentName(info.packageName, info.name) == component
        }
        status.text = when {
            !settings.enabled -> "状态：已暂停"
            !authorized -> "状态：等待开启无障碍服务"
            else -> "状态：已启用，等待 Bilibili 开屏"
        }
        history.text = getString(R.string.history_summary, settings.count, settings.lastClick)
    }

    private fun label(value: String, size: Int) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(Color.rgb(53, 40, 48))
        setPadding(0, dp(12), 0, dp(12))
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        minimumHeight = dp(48)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
