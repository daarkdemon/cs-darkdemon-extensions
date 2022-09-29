
package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.extractors.XStreamCdn
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class AnimeWorldProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://anime-world.in"
    override var name = "Anime World"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.Cartoon
    )

    override val mainPage = mainPageOf(
        "$mainUrl/series/page/" to "Series",
        "$mainUrl/movies/page/" to "Movies",
        "$mainUrl/category/cartoon/page/" to "Cartoon",
        "$mainUrl/category/anime/page/" to "Anime",
        "$mainUrl/category/cartoon-network/page/" to "Cartoon Network",
        "$mainUrl/category/disney/page/" to "Disney",
        "$mainUrl/category/marvel-hq/page/" to "Marvel",
        "$mainUrl/category/hungama-tv/page/" to "Hungama TV",
        "$mainUrl/category/nickelodeon/page/" to "Nickelodeon",
        "$mainUrl/category/sonic/page/" to "Sonic",
        "$mainUrl/category/amazon-prime/page/" to "Amazon Prime",
        "$mainUrl/category/netflix/page/" to "Netflix",
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
        val title = this.selectFirst(".entry-title")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val year = this.select(".year").text().trim().toIntOrNull()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.year = year
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".entry-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst(".post-thumbnail img")?.attr("src"))
        val tags = document.select(".genres a").map { it.text() }
        val year = document.select(".year").text().trim().toIntOrNull()
        val tvType = if (document.select(".episodes  button > span").isNullOrEmpty()
        ) TvType.Movie else TvType.TvSeries
        val description = document.selectFirst(".description > p:nth-child(1)")?.text()?.trim()
        val trailer = fixUrlNull(document.select("iframe").attr("src"))
        val rating = document.select("span.num").text().toRatingInt()
        //val actors = document.select("#cast > div:nth-child(4)").map { it.text() }
        val recommendations = document.select("article").mapNotNull {
            it.toSearchResult()
        }

        return if (tvType == TvType.TvSeries) {
            val episodes : MutableList<Episode> = mutableListOf()
            document.select(".choose-season ul.sub-menu a").mapNotNull { element ->
                val html = app.post(
                    url = "$mainUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "action_select_season",
                        "season" to element.attr("data-season"),
                        "post" to  element.attr("data-post")
                    ),
                    referer = url,
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                ).document

                episodes += html.select("article").mapNotNull {
                    val href = fixUrl(it.select("a").attr("href")?: return null)
                    val name = it.select("h2").text().trim()
                    val thumbs = it.select("img").attr("src")
                    val season = element.attr("data-season").toInt()
                    val episode = it.select(".num-epi").text().split("x").last().toInt()
                    Episode(
                        href,
                        name,
                        season,
                        episode,
                        thumbs
                    )
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document
        val langPair = document.select(".aa-tbs li").map{it.select("a").attr("href").replace("#","") to it.select(".server").text().split("-").last().trim()}.toMap()
        document.select(".aa-cn div").map { res ->
            loadExtractor(
                res.select("iframe").attr("data-src"),
                referer = data,
                subtitleCallback,
            ){ link ->
                callback.invoke(ExtractorLink(
                    link.source,
                    link.name + " " + langPair[res.attr("id")],
                    link.url,
                    link.referer,
                    Qualities.Unknown.value,
                    link.isM3u8,
                    link.headers,
                    link.extractorData
                ))
            }
        }
        return true
    }
}
class Vanfem: XStreamCdn() {
    override val name: String = "Vanfem"
    override val mainUrl: String = "https://vanfem.com"
    override var domainUrl: String = "vanfem.com"
}