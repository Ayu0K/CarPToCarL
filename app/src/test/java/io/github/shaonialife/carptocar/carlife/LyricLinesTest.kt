package io.github.shaonialife.carptocar.carlife

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CarLife 歌词行的识别与拆分。
 *
 * 现实形态有两类：歌词行 `artist == "<歌名>-<歌手>"`，无歌词时三个字段一一对应。
 * 判据直接决定「车机标题栏滚谁」：漏判 = 歌词当歌名（歌手栏出现拼接串），
 * 误判 = 正常歌名被当歌词丢掉（换歌时标题停在上一行）。两边都要钉死。
 */
class LyricLinesTest {
    private fun info(
        song: String = "",
        artist: String = "",
        album: String = "",
        duration: Int = 0,
    ) = CarLifeMediaInfo(
        source = "carlife",
        song = song,
        artist = artist,
        album = album,
        albumArt = null,
        duration = duration,
        playlistNum = 0,
        songId = "",
        mode = 0,
    )

    @Test
    fun `真实歌曲不是歌词行`() {
        val song = info(song = "总有春风为你而来 (女版)", artist = "悠禾", album = "总有春风为你而来 (女版)")
        assertFalse(LyricLines.isLyricLine(song))
    }

    @Test
    fun `画家写成 专辑-画家 就是歌词行`() {
        val lyric = info(song = "要相信所有缘分上天", artist = "总有春风为你而来 (女版)-悠禾", album = "总有春风为你而来 (女版)")
        assertTrue(LyricLines.isLyricLine(lyric))
    }

    @Test
    fun `专辑或画家为空时不算歌词——空基线比误判安全`() {
        assertFalse(LyricLines.isLyricLine(info(song = "某句歌词", artist = "-悠禾", album = "")))
        assertFalse(LyricLines.isLyricLine(info(song = "某句歌词", artist = "", album = "专辑")))
    }

    @Test
    fun `画家只是以艺术家名字开头不算——前缀必须是 专辑- 整体`() {
        // "总有春风为你而来" 是专辑名的前缀但不等于 "专辑-"，不能命中。
        val song = info(song = "总有春风为你而来", artist = "总有春风为你而来不是专辑", album = "总有春风为你而来")
        assertFalse(LyricLines.isLyricLine(song))
    }

    @Test
    fun `歌词行本身是空串也照样认——纯音乐段落的歌词行没有文字`() {
        val lyric = info(song = "", artist = "专辑-悠禾", album = "专辑")
        assertTrue(LyricLines.isLyricLine(lyric))
    }

    @Test
    fun `trueArtist 去掉 专辑- 前缀`() {
        val lyric = info(song = "早有安排", artist = "总有春风为你而来 (女版)-悠禾", album = "总有春风为你而来 (女版)")
        assertEquals("悠禾", LyricLines.trueArtist(lyric))
    }

    @Test
    fun `trueArtist 对真实歌曲原样返回`() {
        val song = info(song = "侧脸", artist = "于果", album = "侧脸")
        assertEquals("于果", LyricLines.trueArtist(song))
    }

    @Test
    fun `歌名与专辑不同也能认出歌词行——网易云实测形态`() {
        val lyric =
            info(
                song = "今追いかけよう",
                artist = "365 Days-蓮ノ空女学院スクールアイドルクラブ",
                album = "Bloom the smile, Bloom the dream!",
            )
        assertTrue(LyricLines.isLyricLine(lyric))
        assertEquals(
            "365 Days" to "蓮ノ空女学院スクールアイドルクラブ",
            LyricLines.splitSongArtist(lyric),
        )
    }

    @Test
    fun `无歌词时三字段一一对应不算歌词行`() {
        val song = info(song = "夜半小夜曲", artist = "李克勤", album = "经典金曲")
        assertFalse(
            LyricLines.isLyricLine(song, sessionTitle = "夜半小夜曲", sessionArtist = "李克勤"),
        )
        assertNull(LyricLines.splitSongArtist(song, sessionArtist = "李克勤"))
    }

    @Test
    fun `真实歌手带连字符不误判——会话歌手作锚点`() {
        val song = info(song = "给我一个理由忘记", artist = "A-Lin", album = "寂寞不痛")
        assertFalse(
            LyricLines.isLyricLine(
                song,
                sessionTitle = "给我一个理由忘记",
                sessionArtist = "A-Lin",
            ),
        )
    }

    @Test
    fun `歌词行优先按会话歌手拆分——歌名自带连字符也不拆错`() {
        val lyric =
            info(
                song = "某句歌词",
                artist = "Re-Re-蓮ノ空女学院スクールアイドルクラブ",
                album = "Bloom the smile, Bloom the dream!",
            )
        assertEquals(
            "Re-Re" to "蓮ノ空女学院スクールアイドルクラブ",
            LyricLines.splitSongArtist(lyric, sessionArtist = "蓮ノ空女学院スクールアイドルクラブ"),
        )
    }

    @Test
    fun `占位歌词识别——纯音乐请欣赏与暂无歌词`() {
        assertTrue(LyricLines.isPlaceholder("纯音乐，请欣赏"))
        assertTrue(LyricLines.isPlaceholder("纯音乐 请欣赏"))
        assertTrue(LyricLines.isPlaceholder("暂无歌词"))
        assertTrue(LyricLines.isPlaceholder("（没有歌词）"))
        assertFalse(LyricLines.isPlaceholder(""))
        assertFalse(LyricLines.isPlaceholder("夜半小夜曲"))
        assertFalse(LyricLines.isPlaceholder("纯音乐治愈系"))
    }
}
