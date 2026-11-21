package io.github.shaonialife.carptocar.launcher.settings

import android.content.Context

/**
 * 「启用 Carlife 媒体歌词」开关（fork 专属）：iAP2 0x5001 NowPlayingUpdate 的
 * **歌名 / 艺术家名数据源二选一**，两条源严格隔离、不互相借用、不互相回退。
 * **封面不参与切换，始终从系统 MediaSession 获取。**
 *
 * 两种取值：
 *  - [Source.CARLIFE]（开）：**始终**从 CarLife 车机侧 0x10035 的 `CarLifeMediaInfo`
 *    读取——标题取 `song`（逐行歌词），歌手取原始 `artist`（歌词行为「歌名-歌手」格式）。
 *    CarLife 的 `albumArt` 一概忽略，封面仍走系统媒体会话。
 *    小米 HyperOS 上 CarWith / Car+ 会抢占系统媒体会话，此时 CarLife 自己的文本元数据更可靠。
 *  - [Source.MEDIA_SESSION]（关）：**始终**从系统 MediaSession 读取真实标题 / 真实歌手；
 *    不接受 CarLife 歌词顶替，CarLife 没推也不回退。封面同样取该系统会话的封面。
 *
 * 播放状态、进度、专辑名保持原有读取路径不变，不受开关影响。
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
