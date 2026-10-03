package com.island.dynamic.settings
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.island.dynamic.theme.StyleHolder
import com.island.dynamic.theme.UiStyle
object SettingsStore {
    private const val PREF = "island_settings"
    private const val KEY_STYLE = "ui_style"
    private const val KEY_AUTO = "auto_switch_sec"
    private const val KEY_INTRO = "intro_shown"
    var autoSwitchSec by mutableStateOf(20); private set
    var introShown by mutableStateOf(false); private set
    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val styleName = sp.getString(KEY_STYLE, UiStyle.GLASS.name) ?: UiStyle.GLASS.name
        StyleHolder.current = runCatching { UiStyle.valueOf(styleName) }.getOrDefault(UiStyle.GLASS)
        autoSwitchSec = sp.getInt(KEY_AUTO, 20)
        introShown = sp.getBoolean(KEY_INTRO, false)
    }
    fun setStyle(context: Context, style: UiStyle) {
        StyleHolder.current = style
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_STYLE, style.name).apply()
    }
    fun setAutoSwitchSec(context: Context, v: Int) {
        autoSwitchSec = v
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putInt(KEY_AUTO, v).apply()
    }
    fun markIntroShown(context: Context) {
        introShown = true
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_INTRO, true).apply()
    }
}
