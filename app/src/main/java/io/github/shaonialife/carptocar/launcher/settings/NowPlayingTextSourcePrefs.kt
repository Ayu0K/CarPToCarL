package io.github.shaonialife.carptocar.launcher.settings

import android.content.Context

/**
 * 「启用 Carlife 媒体歌词」开关（fork 专属）：决定 iAP2 0x5001 NowPlayingUpdate 的
 * **歌词 / 歌名 / 歌手 / 专辑字段映射**，对齐真 iPhone 的三行排版。
 * **封面不参与切换，始终从系统 MediaSession 获取。**
 *
 * 两种取值：
 *  - [Source.CARLIFE]（开）：`title` = CarLife 0x10035 的 `song`（逐行歌词），
 *    `artist` = 系统 MediaSession 的歌名，`album` = MediaSession 的歌手名。
 *    车机三行依次显示「歌词 / 歌名 / 歌手」；系统会话缺席时后两行留空，不回退
 *    CarLife 的 `artist`（歌词行里是「歌名-歌手」拼接串）。
 *  - [Source.MEDIA_SESSION]（关）：`title` / `artist` 取系统 MediaSession 的真实
 *    歌名 / 歌手；`album` 取 CarLife 0x10035 的专辑名（MediaSession 没有专辑字段）。
 *
 * 播放状态、进度、封面保持原有读取路径不变，不受开关影响。
 *
 * 默认 [Source.CARLIFE]：本 fork 的 xiaomi 分支就是为绕开系统媒体会话干扰而做。
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
