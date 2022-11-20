package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.XStreamCdn
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class UWatchFreeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://uwatchfree.be/"
    override var name = "UWatchFree"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/release-year/2022/page/" to "Latest",
        "$mainUrl/uwatchfree-movies/page/" to "Movies",
        "$mainUrl/watch-tv-series/page/" to "Series",
        "$mainUrl/watch-hindi-movies-online/page/" to "Hindi",
        "$mainUrl/tamil-movies/page/" to "Tamil",
        "$mainUrl/telugu-movies/page/" to "Telugu",
        "$mainUrl/hindi-dubbed-movies/page/" to "Hindi Dubbed"
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
        val title = this.selectFirst("h2 a")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val quality = getQualityFromString(this.select(".mli-quality").text())

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query&submit=Search").document

        return document.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".entry-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst(".moviemeta img")?.attr("src"))
        val tags = document.select("div.moviemeta > p:nth-child(4) a").map { it.text() }
        val yearRegex = Regex("""/(\d{4})/gm""")
        val year = yearRegex.find(
            document.select("div.moviemeta > p:nth-child(7)").text()
        )?.groupValues?.getOrNull(1)?.toIntOrNull()
        val description = document.selectFirst("div.moviemeta > p:nth-child(9)")?.text()?.trim()
        val actors =
            document.select("div.moviemeta > p:nth-child(3) span[itemprop=name]").map { it.text() }
        val tvType = if (document.selectFirst(".tritem td:first-child")
                ?.text()
                ?.contains(Regex("(?i)(Episode\\s?[0-9]+)")) == true
        ) TvType.TvSeries else TvType.Movie
        return if (tvType == TvType.TvSeries) {
            val episodes = document.select(".tritem").mapNotNull {
                val href = fixUrl(it.select("a").attr("href") ?: return null)
                val name = it.selectFirst("td")?.text()?.trim()
                val seasonRegex = Regex("""Season\s?([0-9]+)""")
                val season = seasonRegex.find(
                    document.select(".entry-title").text()
                )?.groupValues?.getOrNull(1)?.toIntOrNull()
                val episode = name?.substringAfter("Episode 0")?.substringBefore(":")?.toIntOrNull()
                Episode(
                    href,
                    name,
                    season,
                    episode,
                )
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addActors(actors)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val urls = ArrayList<String>()
        if (data.contains("player")) {
            urls.add(app.get(data).document.select("iframe").attr("src"))
        } else {
            doc.select(".magnet-link a").map { src ->
                if (src.attr("href").contains("send.cm")) {
                    val url = app.get(src.attr("href")).document.select("source").attr("src")
                    urls.add(url)
                }
            }
        }
        doc.select("body a:contains(Click to Play)").map { fixUrl(it.attr("href")) }
            .apmap { source ->
                app.get(
                    source,
                    referer = data
                ).document.select("iframe")
                    .apmap {
                        urls.add(it.attr("src"))
                    }
            }
        println(urls)
        urls.forEach { url ->
            if (url.contains("send.cm")) {
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        url,
                        mainUrl,
                        quality = Qualities.Unknown.value,
                    )
                )
            } else if (url.startsWith("https://0gomovies.top")) {
                val script = app.get(url).text
                println(script)
                val srcRegex = Regex("""(file: ")(https?.*?\.m3u8)""")
                val source =
                    srcRegex.find(script.toString())?.groupValues?.getOrNull(2)
                        ?.toString()
                println(source)
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        source.toString(),
                        referer = url,
                        quality = Qualities.Unknown.value,
                        isM3u8 = true,
                    )
                )
            } else {
                loadExtractor(
                    url,
                    "$mainUrl/",
                    subtitleCallback,
                    callback
                )
            }
        }
        return true
    }
}

class DoodReExtractor : DoodLaExtractor() {
    override var mainUrl = "https://dood.re"
}
