package com.darkdemon

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class MoviesModProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://moviesmod.net"
    override var name = "MoviesMod"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/latest-released/page/" to "Popular Movies",
        "$mainUrl/movies/adult-movies/page/" to "Adult Movies",
        "$mainUrl/tv-series/page/" to "Popular Series",
        "$mainUrl/k-drama/page/" to "K-Drama",
        "$mainUrl/tv-series/hindi-tv-show/" to "Hindi Series",
        "$mainUrl/tv-series/english-tv-shows/page/" to "English Series",
        "$mainUrl/french-web-series/page/" to "French Series",
        "$mainUrl/spanish-series/page/" to "Spanish",
        "$mainUrl/anime/page/" to "Anime"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text()?.replace("""(\(.*${'$'})""".toRegex(), "")
            ?.replace("Download", "")?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(getImageSrc(this.selectFirst("img")!!))
        val quality = getQualityFromString(title)

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select(".post-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title =
            document.selectFirst(".thecontent li")?.text()?.trim()?.replace("Full Name: ", "")
                ?: return null
        val year = """\((\d{4})""".toRegex()
            .find(document.select(".entry-title").text())?.groupValues?.get(1)
            ?.toIntOrNull()
        val description = document.selectFirst(".thecontent > p:nth-child(8)")?.text()?.trim()
        val tvType = if (url.contains("season")) TvType.TvSeries else TvType.Movie
        val href1 =
            if (tvType == TvType.Movie) document.select("h4:contains(1080p), h4:contains(720p)") else document.select(
                "h3:contains(1080p):contains(S0),h3:contains(1080p):contains(Season), h3:contains(720p):contains(S0),h3:contains(720p):contains(Season)"
            )
        val episodeslist = mutableMapOf<Int, MutableList<String>>()
        var num = 0
        val episodeUrls = mutableMapOf<Int, MutableList<String>>()

        val movieLinks = mutableListOf<String>()
        if (tvType == TvType.TvSeries) {
            href1.associate {
                it.text() to it.nextElementSibling()?.firstChild()?.attr("href")
            }.filterValues { it != null }
                .filterValues { it!!.contains("https://episodes.modlinks.xyz") }.map { (k, v) ->
                    val s = """S(\d+)|.eason\s(\d+)""".toRegex().find(k)?.groupValues?.get(0)
                        ?.filter { it.isDigit() }?.toInt()!!

                    if (num == s) episodeUrls[s]?.plusAssign(v!!)
                    else {
                        num = s
                        episodeUrls[s] = mutableListOf(v!!)
                    }
                }
            val episodes = mutableListOf<Episode>()
            episodeUrls.forEach { (season, urls) ->
                var count = 0
                urls.forEach { url ->
                    app.get(url).document.select("h3").mapNotNull { res ->
                        val episode = res.select("strong").text().filter { it.isDigit() }.toInt()
                        val href = res.select("a").attr("href")
                        if (count == season) episodeslist[episode]?.plusAssign(href) else {
                            episodeslist[episode] = mutableListOf(href)
                        }
                    }
                    count = season
                }
                episodeslist.forEach { (e, u) ->
                    episodes += Episode(
                        data = u.toString(),
                        episode = e,
                        season = season
                    )
                }
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.year = year
                this.plot = description
            }

        } else {
            href1.associate {
                it.text() to it.nextElementSibling()?.firstChild()?.attr("href")
            }.filterValues { it != null }
                .filterValues { it!!.contains("modlinks.xyz") }.map { (k, v) ->
                    val s =
                        """(1080)""".toRegex().find(k)?.groupValues?.get(0)?.filter { it.isDigit() }
                            ?.toInt()!!
                    if (num == s) episodeUrls[s]?.plusAssign(v!!)
                    else {
                        num = s
                        episodeUrls[s] = mutableListOf(v!!)
                    }
                }
            episodeUrls[1080]?.map { u ->
                movieLinks += app.get(u).document.select("a:contains(Fast Server),a:contains(Google Drive)")
                    .mapNotNull { it.attr("href") }
            }
            return newMovieLoadResponse(title, url, TvType.Movie, movieLinks) {
                //this.posterUrl = poster
                this.year = year
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        data.splitToSequence(",").toList().forEach {
            val url = it.replace("\"", "").replace("[", "").replace("]", "")
            val source =
                if (it.contains("https://href.li/?")) driveHub(bypassHrefli(url)!!) else driveHub(
                    url
                )
            if (source.contains("404")) return@forEach else
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        source,
                        "",
                        quality = Qualities.P1080.value,
                    )
                )
        }
        return true
    }

    private suspend fun bypassHrefli(url: String): String? {
        val direct = url.removePrefix("https://href.li/?")

        val res = app.get(direct).document
        val formLink = res.select("form#landing").attr("action")
        val wpHttp = res.select("input[name=_wp_http]").attr("value")

        val res2 = app.post(formLink, data = mapOf("_wp_http" to wpHttp)).document
        val formLink2 = res2.select("form#landing").attr("action")
        val wpHttp2 = res2.select("input[name=_wp_http2]").attr("value")
        val token = res2.select("input[name=token]").attr("value")

        val res3 = app.post(
            formLink2, data = mapOf(
                "_wp_http2" to wpHttp2, "token" to token
            )
        ).document

        val script = res3.selectFirst("script:containsData(verify_button)")?.data()
        val directLink = script?.substringAfter("\"href\",\"")?.substringBefore("\")")
        val matchCookies =
            Regex("sumitbot_\\('(\\S+?)',\n|.?'(\\S+?)',").findAll(script ?: return null).map {
                it.groupValues[1] to it.groupValues[2]
            }.toList()

        val cookeName = matchCookies.firstOrNull()?.second ?: return null
        val cookeValue = matchCookies.lastOrNull()?.second ?: return null

        val cookies = mapOf(
            cookeName to cookeValue
        )
        return app.get(
            directLink ?: return null,
            cookies = cookies
        ).document.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=")
    }

    private suspend fun driveHub(url: String): String {
        val domain =
            if (url.startsWith("https://drivehub.in")) "https://drivehub.in" else "http://driveroot.in"
        val path =
            app.get(url, allowRedirects = true).text.substringAfter("/").substringBefore("\"")
        if (path.contains("404")) return path
        val html = app.get("$domain/$path")
        val cookies = html.cookies
        val key = """key",\s+"(.*?)"""".toRegex().find(
            html.document.select("body > script:nth-child(8)").toString()
        )?.groupValues?.get(1)!!
        val fileId = html.document.select("div.card-body > div:nth-child(2) > a").attr("href")
            .substringAfterLast("/")
        val link = app.post(
            url = "$domain/file/${fileId}",
            cookies = cookies,
            data = mapOf(
                "action" to "direct",
                "key" to key,
            ),
            headers = mapOf(
                "Content-Type" to "multipart/form-data; boundary=----",
                "x-token" to domain.substringAfter("//")
            ),
            referer = "$domain/file/${fileId}"
        ).parsed<DriveHub>().url
        return """worker_link\s=\s'(.*mkv)""".toRegex().find(
            app.get(link).document.select("body > script:nth-child(7)").toString()
        )?.groupValues?.get(1).toString()
    }

    private fun getImageSrc(tag: Element): String {
        var image = ""
        val src = tag.attr("src")
        val lazySrc = tag.attr("data-pagespeed-lazy-src")
        val highResSrc = tag.attr("data-pagespeed-high-res-src")
        if (!src.isNullOrEmpty() and !src.startsWith("data") and !src.contains(".gif")) {
            image = tag.attr("src")
        } else if (!lazySrc.isNullOrEmpty() and !lazySrc.startsWith("data") and !lazySrc.contains(".gif")) {
            image = tag.attr("data-pagespeed-lazy-src")
        } else if (!highResSrc.isNullOrEmpty() and !highResSrc.startsWith("data") and !highResSrc.contains(
                ".gif"
            )
        ) {
            image = tag.attr("data-pagespeed-high-res-src")
        }
        return image
    }

    data class DriveHub(
        @JsonProperty("url") var url: String

    )
}
