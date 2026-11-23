package io.github.shaonialife.carptocar.iap2

import io.github.shaonialife.carptocar.carlife.CarLifeMediaInfo
import io.github.shaonialife.carptocar.carlife.LyricLines

/** 0x5001 NowPlayingUpdate 的三行文本字段。 */
data class NowPlayingFields(
    val title: String?,
    val artist: String?,
    val album: String?,
)

/**
 * 「启用 Carlife 媒体歌词」打开时，0x5001 三行字段的取数策略。
 *
 * 目标是让车机三行尽量像一台真 iPhone：
 *
 * - **有歌词且系统会话可读**：title = CarLife 当前歌词（`song`），artist = 系统会话的
 *   **完整歌名**，album = 系统会话的**歌手**。这是实车验证过与 iPhone 一致的旧实现：
 *   保留播放器追加在标题里的翻译/备注（例如「365 Days (莲之空女学院学园偶像俱乐部
 *   第三张单曲C/W曲)」），只把歌词单独提到第一行。
 * - **有歌词但系统会话读不到**：才从 CarLife `artist`（形如「歌名-歌手」）拆出真实歌名
 *   / 真实歌手，分别填 artist / album，至少不让三行空着。
 * - **无歌词 / 占位歌词**（如「纯音乐，请欣赏」）：回退到 MediaSession 的真实歌名
 *   与真实歌手，album 仍取 CarLife 的专辑名。
 * - MediaSession 缺席时，尽量用 CarLife 自己拆出的歌名/歌手，避免三行全空。
 *
 * 只算文本字段；播放状态、进度、封面仍由调用方决定，封面始终取 MediaSession。
 */
object NowPlayingFieldPolicy {
    fun carLife(
        info: CarLifeMediaInfo,
        sessionTitle: String?,
        sessionArtist: String?,
        carLifeAlbum: String?,
    ): NowPlayingFields {
        val lyric = LyricLines.isLyricLine(info, sessionTitle, sessionArtist)
        val placeholder = LyricLines.isPlaceholder(info.song)
        val split = if (lyric) LyricLines.splitSongArtist(info, sessionArtist) else null
        val lyricTitle = info.song.trim().takeIf { it.isNotEmpty() }

        if (lyric && !placeholder) {
            // 旧实现（实车验证与 iPhone 一致）：歌名/歌手直接取系统会话原值，
            // 播放器追加在歌名后的备注一并保留，特地把歌词提到第一行。
            if (!sessionTitle.isNullOrBlank() && !sessionArtist.isNullOrBlank()) {
                return NowPlayingFields(
                    title = lyricTitle,
                    artist = sessionTitle.trim(),
                    album = sessionArtist.trim(),
                )
            }
            // 系统会话读不到时，才从 CarLife 的「歌名-歌手」拆出兜底值。
            return NowPlayingFields(
                title = lyricTitle,
                artist = firstNonBlank(split?.first, sessionTitle),
                album = firstNonBlank(split?.second, sessionArtist, carLifeAlbum),
            )
        }

        // 回退：系统会话的歌名/歌手 + CarLife 专辑；会话缺席时用 CarLife 的拆分结果兜底。
        return NowPlayingFields(
            title = firstNonBlank(sessionTitle, split?.first, info.song),
            artist = firstNonBlank(sessionArtist, split?.second, info.artist),
            album = firstNonBlank(carLifeAlbum, info.album),
        )
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()
}
