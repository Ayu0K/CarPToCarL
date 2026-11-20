package io.github.shaonialife.carptocar.launcher.settings

import android.content.Context
import android.os.Build

/**
 * 占位保活 MediaSession 的总开关（fork: CarPToCarL-MediaSessionSwitch）。
 *
 * 背景：原版为了 MagicOS 后台保活，会常驻发布一条 tag 为 `CarPToCarL` 的
 * MediaSession。在小米 HyperOS / OPPO ColorOS 上，这条会话会被 CarWith / Car+
 * 当成当前媒体源，导致内置车联媒体卡片被独占（歌名不显示、按键控制失效）。
 * Android 没有 per-session 的可见性开关，因此只能在源头控制要不要发布。
 *
 * 默认策略：
 *  - 荣耀 / MagicOS：默认开启（保活是该会话的初衷，不破坏原机型行为）
 *  - 其它 ROM：默认关闭（避免干扰内置车联）
 * 首次读取时把默认值落盘，之后完全以用户选择为准；界面开关可随时覆盖。
 */
object KeepAliveMediaSessionPrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_ENABLED = "keep_alive_media_session_enabled"

    /** 只按厂商/品牌判断，不依赖隐藏 API；MagicOS 机型两者至少有一个为 HONOR。 */
    fun defaultEnabled(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        return manufacturer.equals("HONOR", ignoreCase = true) ||
            brand.equals("HONOR", ignoreCase = true)
    }

    fun enabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_ENABLED)) {
            prefs.getBoolean(KEY_ENABLED, false)
        } else {
            defaultEnabled().also { prefs.edit().putBoolean(KEY_ENABLED, it).apply() }
        }
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
