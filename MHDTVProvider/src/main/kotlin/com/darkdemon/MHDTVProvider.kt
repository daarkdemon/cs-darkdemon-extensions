package com.darkdemon

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.math.floor

class MHDTVProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://mhdtvworld.xyz"
    override var name = "MHDTVWorld"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = false
    override val supportedTypes = setOf(
        TvType.Live
    )

    override val mainPage = mainPageOf(
        "$mainUrl/channel/sports/page/" to "Sports",
        "$mainUrl/channel/english/page/" to "English",
        "$mainUrl/channel/hindi/page/" to "Hindi",
        "$mainUrl/channel/marathi/page/" to "Marathi",
        "$mainUrl/channel/tamil/page/" to "Tamil",
        "$mainUrl/channel/telugu/page/" to "Telugu",
        "$mainUrl/channel/malayalam/page/" to "Malayalam",
        "$mainUrl/channel/malayalam-news/page/" to "Malayalam News",
        "$mainUrl/channel/kannada/page/" to "Kannada",
        "$mainUrl/channel/punjabi/page/" to "Punjabi",
        "$mainUrl/channel/bangla/page/" to "Bangla",
        "$mainUrl/channel/hindi-movies/page/" to "Hindi Movies",
        "$mainUrl/channel/malayalam-movies/page/" to "Malayalam Movies",
        "$mainUrl/channel/pakistani/page/" to "Pakistani TV",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (request.data.contains("page")) {
            request.data + page
        } else {
            request.data
        }
        val document = app.get(url).document
        val home = document.select("article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("img")?.attr("alt")?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Live) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    private suspend fun getPostUrl(url: String, post: String, nume: String, type: String): String {
        return app.post(
            url = "$mainUrl/wp-admin/admin-ajax.php",
            data = mapOf(
                "action" to "doo_player_ajax",
                "post" to post,
                "nume" to nume,
                "type" to type
            ),
            referer = url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).parsed<ResponseHash>().embed_url
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst(".poster img")?.attr("src"))
        val episodes = document.select("ul#playeroptionsul li").mapIndexedNotNull { index, it ->
            val name = it.selectFirst(".title")?.text()?.trim()
            val post = it.attr("data-post")
            val nume = it.attr("data-nume")
            val type = it.attr("data-type")
            val thumbs = it.select(".flag img").attr("src")
            val href = getPostUrl(url, post, nume, type)
            val link =
                if (href.contains("video")) Jsoup.parse(href).select("source").attr("src")
                else if (href.contains("iframe")) Jsoup.parse(href).select("iframe").attr("src")
                else if (href.startsWith("/delta")) "$mainUrl$href"
                else href
            Episode(
                link,
                name,
                season = 1,
                episode = index + 1,
                posterUrl = thumbs
            )
        }
        return if (!document.select(".sgeneros a").last()?.text()!!.contains("movies")) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
            }
        } else {
            val doc = document.select("ul#playeroptionsul li").mapIndexedNotNull { _, it ->
                val post = it.attr("data-post")
                val nume = it.attr("data-nume")
                val type = it.attr("data-type")
                getPostUrl(url, post, nume, type)
            }
            val trailer = if (doc.size == 1) "" else doc[0]
            val href = if (doc.size == 1) doc[0] else doc[1]
            val link = if (href.startsWith("/delta")) "$mainUrl$href" else href
            return newMovieLoadResponse(title, url, TvType.Movie, link) {
                this.posterUrl = poster
                addTrailer(trailer)
            }
        }
    }

    private fun decode(input: String): String = java.net.URLDecoder.decode(input, "utf-8")

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(url = data, referer = "$mainUrl/").document
        if (data.startsWith("https://mhdtvworld.xyz/jwplayer/")) {

            val decoded = decode(data)
            val source = decoded.substringAfter("source=").substringBefore("&id")
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    url = source,
                    referer = data,
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )
        } else if (data.startsWith("https://mhdtvworld.xyz/delta") || data.startsWith("https://yuppstream.net.in/")) {
            val srcRegex = Regex("""hls: '(.*?.)',""")
            val regexMatch =
                srcRegex.find(document.toString())?.groupValues?.getOrNull(1).toString()
            val source =
                if (!regexMatch.startsWith("https")) "${data.substringBefore("/play.php")}$regexMatch" else regexMatch
            val referer =
                if (data.startsWith(mainUrl)) "$mainUrl/" else data.substringBefore("play.php")
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    url = source,
                    referer = referer,
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )

        } else if (data.contains("m3u8")) {
            ExtractorLink(
                this.name,
                this.name,
                url = data.trim(),
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        } else if (data.startsWith("https://tvstream.fun") || data.startsWith("https://tvworld.fun/") || data.startsWith(
                "https://tv.googledrivelinks.com/"
            )
        ) {
            val domain =
                if (data.startsWith("https://tvstream.fun")) data.substringBefore("/jtv") else data.substringBefore(
                    "/play.php"
                )
            val srcRegex = Regex("""source: "(.*?.)",|hls: '(.*?.)',""")
            val source =
                if (!srcRegex.find(document.toString())?.groupValues?.getOrNull(2).isNullOrEmpty())
                    "$domain${
                        srcRegex.find(document.toString())?.groupValues?.getOrNull(2)
                    }" else if (document.select("source").attr("src")
                        .startsWith("/")
                ) "$domain${document.select("source").attr("src")}" else "$domain/${
                    document.select(
                        "source"
                    ).attr("src")
                }"
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    source,
                    referer = data,
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )
        } else if (data.startsWith("https://techclips.net/")) {
            val domain = data.substringBefore("clip/")
            val srcRegex = Regex("""servs = \[(.*?.)];""")
            val servers = srcRegex.find(document.toString())?.groupValues?.getOrNull(1)?.split(",")
            val server =
                servers?.get(floor(Math.random() * servers.size).toInt())?.replace("\"", "")
                    ?.trim()

            val source = "https://$server${data.substringAfterLast("/").replace("html", "m3u8")}"
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    source,
                    referer = domain,
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )
        } else if (data.startsWith("https://gocast123.me/")) {
            val srcRegex = Regex("""source: '(https?.*?.m3u8)',""")
            val source =
                srcRegex.find(document.toString())?.groupValues?.getOrNull(1).toString()
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    url = source,
                    referer = "https://123ecast.me/",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )
        } else if (data.startsWith("https://v4.sportsonline.to")) {
            val embedLink = document.select("iframe").attr("src")
            app.get(
                embedLink,
                referer = data.substringBefore("channels")
            ).document.select("script").map { it.data() }
                .filter { it.contains("eval(function(p,a,c,k,e,d)") }
                .map { script ->
                    val unpacked = if (script.contains("m3u8")) getAndUnpack(script) else ""
                    val link =
                        Regex("""[src="](https?.*?)"""").findAll(unpacked).map { it.value }.toList()
                            .toList()
                    val domain = embedLink.substringBefore("embed")
                    link.forEach {
                        callback.invoke(
                            ExtractorLink(
                                this.name,
                                this.name,
                                it.replace("\"", ""),
                                domain,
                                Qualities.Unknown.value,
                                isM3u8 = true
                            )
                        )
                    }
                }
        }
        return true
    }

    private fun fixUrl(url: String): String {
        if (url.isEmpty()) return ""

        if (url.startsWith("//")) {
            return "http:$url"
        }
        if (!url.startsWith("http")) {
            return "http://$url"
        }
        return url
    }

    data class ResponseHash(
        @JsonProperty("embed_url") val embed_url: String,
        @JsonProperty("type") val type: String?,
    )
}
