package io.github.shaonialife.carptocar.carlife

/**
 * CarLife 歌词行的识别、占位歌词判定与「歌名-歌手」拆分。
 *
 * CarLife 引擎**没有专门的歌词通道**：`CarlifeMediaInfo` 的字段只有
 * `source/song/artist/album/albumArt/duration/playlistNum/songId/mode`
 * （见 [CarLifeProtocol.parseMediaInfoList]），逐行歌词是**另起一条媒体信息**
 * 推过来的。实测有两种形态：
 *
 * ```
 * 歌词行：song=今追いかけよう  artist=365 Days-蓮ノ空女学院スクールアイドルクラブ  album=Bloom the smile, Bloom the dream!
 * 无歌词：song=夜半小夜曲      artist=李克勤                                  album=经典金曲
 * ```
 *
 * 即：**歌词行的 `artist` 是 `<歌名>-<歌手>`，无歌词时三个字段一一对应**。
 * 早期文档写的 `<专辑>-<歌手>` 只是「歌名与专辑同名」时的特例（如
 * `总有春风为你而来 (女版)-悠禾`）；专辑名与歌名不同的曲目会把这个判据骗过去，
 * 所以现在优先用系统 MediaSession 的歌手做锚点，再退回结构判断。
 *
 * 只做识别 / 拆分，不决定展示什么——展示策略在
 * [io.github.shaonialife.carptocar.iap2.NowPlayingFieldPolicy]。
 */
object LyricLines {
    /**
     * 这条媒体信息是不是「歌词行」。
     *
     * @param sessionTitle 系统 MediaSession 当前的歌名（可空）。无歌词时 CarLife 的
     *   `song` 与之相同；歌词行时 `song` 是某一句歌词，通常不同。
     * @param sessionArtist 系统 MediaSession 当前的歌手（可空）。歌词行的 `artist`
     *   以 `-<sessionArtist>` 结尾；无歌词时 `artist` 就等于它。
     *
     * 判定顺序（越靠前越可靠）：
     * 1. `artist == sessionArtist` → 真实歌曲（一一对应）；
     * 2. `artist` 以 `-<sessionArtist>` 结尾 → 歌词行；
     * 3. 旧判据 `artist` 以 `<album>-` 开头 → 歌词行（歌名==专辑的场景）；
     * 4. 没有会话信息时看结构：`artist` 含「-」，且前缀不是当前 `song`
     *    （当前 `song` 是歌词，前缀才是真实歌名）。
     */
    fun isLyricLine(
        info: CarLifeMediaInfo,
        sessionTitle: String? = null,
        sessionArtist: String? = null,
    ): Boolean {
        val song = info.song.trim()
        val artist = info.artist.trim()
        val album = info.album.trim()
        if (artist.isEmpty()) return false

        val knownArtist = sessionArtist?.trim()
        if (!knownArtist.isNullOrEmpty()) {
            // 一一对应：CarLife 的歌手就是系统会话歌手 → 不是歌词行。
            if (artist == knownArtist) return false
            // 歌词行：artist = "<真实歌名>-<真实歌手>"，歌手部分与会话一致。
            if (artist.length > knownArtist.length + 1 && artist.endsWith("-$knownArtist")) {
                return true
            }
        }

        // 旧判据：歌名与专辑同名时，artist = "<专辑>-<歌手>"。
        if (album.isNotEmpty() && artist.length > album.length + 1 && artist.startsWith("$album-")) {
            return true
        }

        // 无 MediaSession 时的结构判据：artist 含「-」且前缀不是当前歌词行文本。
        // 前缀至少 2 个字符，避免把 "A-Lin" 这类歌手名误当成「歌名-歌手」。
        if (song.isNotEmpty()) {
            val hyphen = artist.lastIndexOf('-')
            if (hyphen > 0 && hyphen < artist.length - 1) {
                val prefix = artist.substring(0, hyphen).trim()
                val suffix = artist.substring(hyphen + 1).trim()
                if (prefix.length >= 2 && suffix.isNotEmpty() && prefix != song) return true
            }
        }
        return false
    }

    /**
     * 从歌词行的 `artist` 拆出「真实歌名 / 真实歌手」。
     *
     * 优先用 [sessionArtist] 定位分隔符（`artist` 必须以 `-<sessionArtist>` 结尾），
     * 这样歌名或歌手自身带「-」也不会拆错；没有会话歌手时依次退回
     * `<album>-` 前缀和最后一个「-」。不是歌词行 / 拆不出来时返回 null。
     */
    fun splitSongArtist(
        info: CarLifeMediaInfo,
        sessionArtist: String? = null,
    ): Pair<String, String>? {
        val artist = info.artist.trim()
        if (artist.isEmpty()) return null

        val knownArtist = sessionArtist?.trim()
        if (!knownArtist.isNullOrEmpty() &&
            artist.length > knownArtist.length + 1 &&
            artist.endsWith("-$knownArtist")
        ) {
            val song = artist.removeSuffix("-$knownArtist").trim()
            if (song.isNotEmpty()) return song to knownArtist
        }

        val album = info.album.trim()
        if (album.isNotEmpty() && artist.length > album.length + 1 && artist.startsWith("$album-")) {
            val trueArtist = artist.removePrefix("$album-").trim()
            if (trueArtist.isNotEmpty()) return album to trueArtist
        }

        val hyphen = artist.lastIndexOf('-')
        if (hyphen > 0 && hyphen < artist.length - 1) {
            val song = artist.substring(0, hyphen).trim()
            val trueArtist = artist.substring(hyphen + 1).trim()
            if (song.isNotEmpty() && trueArtist.isNotEmpty()) return song to trueArtist
        }
        return null
    }

    /**
     * 无歌词时播放器推送的占位文案，如「纯音乐，请欣赏」「暂无歌词」。
     *
     * 归一化只去空白与标点，避免全角/半角逗号、括号差异导致漏判。
     * **空串不算占位**：纯音乐段落的歌词行本身可能就是空串，此时应保留歌词行形态，
     * 不能回退成歌名（否则间奏会每秒在歌词/歌名之间闪）。
     */
    fun isPlaceholder(song: String?): Boolean {
        val normalized = normalize(song.orEmpty())
        if (normalized.isEmpty()) return false
        if (normalized.contains("暂无歌词") || normalized.contains("无歌词") || normalized.contains("没有歌词")) {
            return true
        }
        if (normalized == "纯音乐" || normalized == "纯音乐请欣赏") return true
        return normalized.startsWith("纯音乐") && normalized.contains("欣赏")
    }

    /**
     * 歌词行里真正的画家名：取 [splitSongArtist] 的后半段；拆不出来时原样返回入参。
     */
    fun trueArtist(info: CarLifeMediaInfo): String = splitSongArtist(info)?.second ?: info.artist

    /** 去掉空白与标点后的小写形式，用于占位文案匹配。 */
    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("[\\s\\p{P}\\p{S}]"), "")
}
