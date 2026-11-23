package io.github.shaonialife.carptocar.iap2

import io.github.shaonialife.carptocar.carlife.CarLifeMediaInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 「启用 Carlife 媒体歌词」开启时的 0x5001 三行字段策略。
 *
 * 三条用例直接对应实车观察到的现象：有歌词（网易云标题带备注）、
 * 纯音乐占位歌词、播放器不输出歌词时三字段一一对应。
 */
class NowPlayingFieldPolicyTest {
    private val band = "蓮ノ空女学院スクールアイドルクラブ"
    private val album = "Bloom the smile, Bloom the dream!"

    private fun info(
        song: String,
        artist: String,
        album: String,
    ) = CarLifeMediaInfo(
        source = "网易云音乐",
        song = song,
        artist = artist,
        album = album,
        albumArt = null,
        duration = 240_000,
        playlistNum = 0,
        songId = "",
        mode = 0,
    )

    @Test
    fun `有歌词——歌词当标题，歌名歌手取系统会话完整值（保留备注）`() {
        val fields =
            NowPlayingFieldPolicy.carLife(
                info = info(song = "今追いかけよう", artist = "365 Days-$band", album = album),
                // 旧实现：播放器追加在标题后的翻译/备注一并推送，实车验证与 iPhone 一致。
                sessionTitle = "365 Days (莲之空女学院学园偶像俱乐部第三张单曲C-W曲)",
                sessionArtist = band,
                carLifeAlbum = album,
            )

        assertEquals("今追いかけよう", fields.title)
        assertEquals("365 Days (莲之空女学院学园偶像俱乐部第三张单曲C-W曲)", fields.artist)
        assertEquals(band, fields.album)
    }

    @Test
    fun `有歌词但会话读不到——从 CarLife 歌名-歌手拆分兜底`() {
        val fields =
            NowPlayingFieldPolicy.carLife(
                info = info(song = "今追いかけよう", artist = "365 Days-$band", album = album),
                sessionTitle = null,
                sessionArtist = null,
                carLifeAlbum = album,
            )

        assertEquals("今追いかけよう", fields.title)
        assertEquals("365 Days", fields.artist)
        assertEquals(band, fields.album)
    }

    @Test
    fun `占位歌词——回退系统歌名歌手，专辑仍取 CarLife`() {
        val fields =
            NowPlayingFieldPolicy.carLife(
                info =
                    info(
                        song = "纯音乐，请欣赏",
                        artist = "365 Days (Off Vocal)-$band",
                        album = album,
                    ),
                sessionTitle = "365 Days (莲之空女学院学园偶像俱乐部第三张单曲C-W曲)",
                sessionArtist = band,
                carLifeAlbum = album,
            )

        assertEquals("365 Days (莲之空女学院学园偶像俱乐部第三张单曲C-W曲)", fields.title)
        assertEquals(band, fields.artist)
        assertEquals(album, fields.album)
    }

    @Test
    fun `无歌词一一对应——系统歌名歌手 + CarLife 专辑`() {
        val fields =
            NowPlayingFieldPolicy.carLife(
                info = info(song = "夜半小夜曲", artist = "李克勤", album = "经典金曲"),
                sessionTitle = "夜半小夜曲",
                sessionArtist = "李克勤",
                carLifeAlbum = "经典金曲",
            )

        assertEquals("夜半小夜曲", fields.title)
        assertEquals("李克勤", fields.artist)
        assertEquals("经典金曲", fields.album)
    }

    @Test
    fun `会话缺席——占位歌词仍用 CarLife 拆出的歌名歌手，不把拼接串塞进歌手栏`() {
        val fields =
            NowPlayingFieldPolicy.carLife(
                info =
                    info(
                        song = "纯音乐，请欣赏",
                        artist = "365 Days (Off Vocal)-$band",
                        album = album,
                    ),
                sessionTitle = null,
                sessionArtist = null,
                carLifeAlbum = album,
            )

        assertEquals("365 Days (Off Vocal)", fields.title)
        assertEquals(band, fields.artist)
        assertEquals(album, fields.album)
    }

    @Test
    fun `会话缺席且无歌词——三字段原样使用 CarLife`() {
        val fields =
            NowPlayingFieldPolicy.carLife(
                info = info(song = "夜半小夜曲", artist = "李克勤", album = "经典金曲"),
                sessionTitle = null,
                sessionArtist = null,
                carLifeAlbum = "经典金曲",
            )

        assertEquals("夜半小夜曲", fields.title)
        assertEquals("李克勤", fields.artist)
        assertEquals("经典金曲", fields.album)
    }
}
