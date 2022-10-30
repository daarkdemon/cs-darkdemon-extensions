
package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class UWatchFreeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://uwatchfree.fan/"
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
        val year = yearRegex.find(document.select("div.moviemeta > p:nth-child(7)").text())?.groupValues?.getOrNull(1)?.toIntOrNull()
        val description = document.selectFirst("div.moviemeta > p:nth-child(9)")?.text()?.trim()
        val actors = document.select("div.moviemeta > p:nth-child(3) span[itemprop=name]").map { it.text() }

        /*return if (tvType == TvType.TvSeries) {
            val episodes = if (document.selectFirst("div.les-title strong")?.text().toString()
                    .contains("Season")
            ) {
                document.select("ul.idTabs li").map {
                    val id = it.select("a").attr("href")
                    Episode(
                        data = fixUrl(document.select("div$id iframe").attr("src")),
                        name = it.select("strong").text()
                    )
                }
            } else {
                document.select("div.les-content a").map {
                    Episode(
                        data = it.attr("href"),
                        name = it.text(),
                    )
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                addActors(actors)
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        } else {*/
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        app.get(data).document.select("body a:contains(Click to Play)").map { fixUrl(it.attr("href")) }
            .apmap { source ->
                safeApiCall {
                    when {
                        source.startsWith(mainUrl) -> app.get(
                            source,
                            referer = data
                        ).document.select("#res_video_player_div iframe")
                            .apmap {
                                loadExtractor(
                                    it.attr("src"),
                                    "$mainUrl/",
                                    subtitleCallback,
                                    callback
                                )
                            }
                        else -> {}
                    }
                }
            }
        return true
    }
}

