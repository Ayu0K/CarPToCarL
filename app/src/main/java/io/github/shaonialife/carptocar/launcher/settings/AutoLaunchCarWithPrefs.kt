package io.github.shaonialife.carptocar.launcher.settings

import android.content.Context
import android.os.Build

/**
 * 「自动拉起 CarWith 进入 CarLife 连接」开关（fork: CarPToCarL-MediaSessionSwitch）。
 *
 * 背景：本项目会守候本机 CarLife 引擎的 7240 端口并接管其画面/音频。小米 HyperOS 上，
 * CarWith 的 CarLife 连接入口（`com.miui.carlink.castfwk.usb.CarlifeAdbActivity`）是已导出
 * Activity，可用普通 Intent 拉起，让它自动完成无线连接并开始监听七口；这样用户不必每次
 * 手动打开 CarWith 点「无线连接」。移植自 mcarplay 的验证实现。
 *
 * 默认策略：
 *  - 小米 / Redmi / POCO：默认开启（该入口在小米上实测可用，是这个 fork 的目标机型）
 *  - 其它 ROM（含荣耀 MagicOS）：默认关闭。荣耀上自动拉起只能拿到「Powered by MagicOS」
 *    欢迎页静止帧、拿不到真实画面（历史回滚结论），故不自动拉起。
 * 首次读取时把默认值落盘，之后完全以用户选择为准；界面开关可随时覆盖。
 */
object AutoLaunchCarWithPrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_ENABLED = "auto_launch_carwith_enabled"

    /** 只按厂商/品牌字符串判断，不依赖隐藏 API。红米的 MANUFACTURER 通常也是 Xiaomi。 */
    fun defaultEnabled(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        return isXiaomi(manufacturer) || isXiaomi(brand)
    }

    private fun isXiaomi(value: String): Boolean =
        value.equals("Xiaomi", ignoreCase = true) ||
            value.equals("Redmi", ignoreCase = true) ||
            value.equals("POCO", ignoreCase = true)

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
