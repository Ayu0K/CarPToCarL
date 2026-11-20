package io.github.shaonialife.carptocar.launcher.settings

import android.content.Context

/**
 * iAP2 0x5001 NowPlayingUpdate 里**歌名 / 艺术家名**的数据源开关（fork 专属）。
 *
 * 两种取值：
 *  - [Source.CARLIFE]：取 CarLife 车机侧 0x10035 推来的媒体信息。小米 HyperOS 上
 *    CarWith / Car+ 会抢占系统媒体会话，第三方播放器的歌名常常读不到或串台，
 *    这时 CarLife 内置播放器推来的元数据反而更可靠。
 *  - [Source.MEDIA_SESSION]：原版行为，优先读系统 MediaSession（通知使用权），
 *    覆盖第三方播放器的绝大多数场景。
 *
 * **只影响歌名与艺术家名**：播放状态、进度、专辑名，以及封面（始终来自
 * `MediaSessionsState.nowPlaying?.art`）全部保持原有读取路径不变。
 *
 * 默认 [Source.CARLIFE]：本 fork 的 xiaomi 分支就是为绕开系统媒体会话干扰而做。
 * CarLife 没有推媒体信息时，[io.github.shaonialife.carptocar.SessionController]
 * 仍会回退到 MediaSession，不会让车机歌名变空。
 */
object NowPlayingTextSourcePrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_SOURCE = "now_playing_text_source"

    enum class Source {
        CARLIFE,
        MEDIA_SESSION,
        ;

        val isCarLife: Boolean get() = this == CARLIFE
    }

    fun current(context: Context): Source {
        val raw =
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SOURCE, null)
        return when (raw) {
            "media_session" -> Source.MEDIA_SESSION
            else -> Source.CARLIFE
        }
    }

    fun set(
        context: Context,
        source: Source,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOURCE, if (source.isCarLife) "carlife" else "media_session")
            .apply()
    }
}
