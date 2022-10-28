package com.darkdemon

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.jsoup.nodes.Element

class NollyVerseProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://www.nollyverse.com"
    override var name = "NollyVerse"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama
    )

    private fun serializeData(element: Element): List<NollyVerseLink> {

        val parsed = element.select("td").mapNotNull {
            try {
                val name = if (element.select("tr > td:eq(0)").text()
                        .contains("Episode", ignoreCase = true)
                ) it.text()
                else it.select("a").text()
                val url = it.select("a").attr("href")
                NollyVerseLink(name, url)
            } catch (e: Exception) {
                NollyVerseLink("", "")
            }
        }.filter { it.link != "" && it.name != "" }
        return parsed.reversed()
    }

    data class NollyVerseLink(
        @JsonProperty("name") val name: String,
        @JsonProperty("url") val link: String
    )

    override val mainPage = mainPageOf(
        "$mainUrl/category/latest-movies/page/" to "Latest Movies",
        "$mainUrl/category/new-series/page/" to "Latest Series",
        "$mainUrl/category/popular-movies/page/" to "Popular Movies",
        "$mainUrl/category/korean-movies/page/" to "Korean Movies",
        "$mainUrl/category/korean-series/page/" to "Korean Series"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val selector = if (request.data.contains("korean")) {
            ".col-md-8 .post"
        } else if (request.data.contains("popular")) {
            ".col-md-8 .post"
        } else {
            ".post-row"
        }
        val home = document.select(selector).mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".post-title a")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst(".post-img")?.attr("href").toString())
        val posterUrl = if (fixUrlNull(
                this.selectFirst(".post-img img")?.attr("src")
            )?.contains("blank") == true
        ) fixUrlNull(
            this.selectFirst(".post-img img")
                ?.attr("data-src")
        ) else fixUrlNull(this.selectFirst(".post-img img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post(
            url = "$mainUrl/livesearch.php",
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"
            ),
            data = mapOf("name" to query)
        ).document
        return document.select("a").mapNotNull {
            val title = it.text().trim()
            val href = fixUrl(it.attr("href").toString())
            val posterUrl =
                fixUrlNull("https://i.ibb.co/fdnLwRf/istockphoto-1071359118-612x612.jpg")
            val tvtype = if (href.contains("serie")) TvType.TvSeries else TvType.Movie
            newMovieSearchResponse(title, href, tvtype) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.select("ol li").last()?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[itemprop=image]")?.attr("content"))
        val tags =
            document.select("p:contains(Genre:)").text().substringAfter(" ").split(",").map { it }
        val year = Regex("([0-9]{4}?)").find(
            document.select("p:contains(Release Date:)").text().toString()
        )?.groupValues?.get(1)?.toIntOrNull()
        val tvType = if (document.select("table")
                .isNullOrEmpty()
        ) TvType.Movie else TvType.TvSeries
        val description = document.selectFirst(".blockquote > small")?.text()?.trim()
        val trailer = fixUrlNull(
            "https://www.youtube.com/embed/" + document.selectFirst(".youtube")?.attr("data-embed")
        )
        val actors =
            document.select("p:contains(Stars:)").text().substringAfter(": ").split(",").map { it }
        val recommendations = document.select(".galery-widget ul a").mapNotNull {
            val title =
                document.selectFirst("img")?.attr("alt")?.substringBefore("-") ?: return null
            val href = fixUrl(document.attr("href").toString())
            val posterUrl = fixUrlNull(document.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }

        return if (tvType == TvType.TvSeries) {
            val seasons = document.select("tr a.btn-sm").mapNotNull { it.attr("href") }
            val episodes = ArrayList<Episode>()
            for (s in seasons) {
                val document = app.get(s).document
                document.select("tbody tr").mapNotNull {
                    val season = s.substringAfter("-").toIntOrNull()
                    val links = it
                    val data = serializeData(links)
                    episodes.add(newEpisode(data) {
                        this.season = season
                        this.episode = episode
                    })
                }
            }
            episodes.reverse()
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
        } else {
            val doc = app.get(document.select(".section-row .row a").attr("href")).document
            val serialize =
                doc.selectFirst("tbody") ?: throw ErrorLoadingException("No links found")
            newMovieLoadResponse(title, url, TvType.Movie, serializeData(serialize)) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addActors(actors)
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

        val links = parseJson<List<NollyVerseLink>>(data)
        for (link in links) {
            val parsedLink = if (link.link.contains("anonfiles.com")) {
                app.get(link.link).document.selectFirst("#download-url")?.attr("href")
            } else {
                link.link
            } ?: return false
            val urlName = Regex("([0-9]+p)").find(parsedLink)?.groupValues?.get(1).toString()
            callback.invoke(
                ExtractorLink(
                    this.name,
                    urlName,
                    parsedLink,
                    "",
                    getQualityFromName(parsedLink),
                    false
                )
            )
        }
        return true
    }
}
