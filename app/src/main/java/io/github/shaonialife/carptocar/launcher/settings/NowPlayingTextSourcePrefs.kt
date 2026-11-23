package io.github.shaonialife.carptocar.launcher.settings

import android.content.Context

/**
 * 「启用 Carlife 媒体歌词」开关（fork 专属）：决定 iAP2 0x5001 NowPlayingUpdate 的
 * **歌词 / 歌名 / 歌手 / 专辑字段映射**，对齐真 iPhone 的三行排版。
 * **封面不参与切换，始终从系统 MediaSession 获取。**
 *
 * 两种取值：
 *  - [Source.CARLIFE]（开）：有歌词时 `title` = CarLife 的当前歌词（`song`），
 *    `artist` / `album` = 从 CarLife `artist`（形如「歌名-歌手」）拆出的真实歌名 / 歌手；
 *    无歌词或占位歌词（「纯音乐，请欣赏」）时回退系统 MediaSession 的歌名 / 歌手，
 *    `album` 仍取 CarLife 的专辑名。车机三行因此始终有完整内容。
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
