package io.github.bilisplashskip

import android.content.Context
import java.text.DateFormat
import java.util.Date

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    val count: Int get() = prefs.getInt("count", 0)
    val lastClick: String
        get() = prefs.getLong("last_click", 0L).let {
            if (it == 0L) "尚无记录" else DateFormat.getDateTimeInstance().format(Date(it))
        }

    fun recordClick() {
        prefs.edit().putInt("count", count + 1)
            .putLong("last_click", System.currentTimeMillis()).apply()
    }

    fun clearHistory() { prefs.edit().remove("count").remove("last_click").apply() }
}
