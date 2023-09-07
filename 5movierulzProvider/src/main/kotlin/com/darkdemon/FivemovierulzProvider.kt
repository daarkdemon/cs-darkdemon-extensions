package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.extractors.StreamSB
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.XStreamCdn
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class FivemovierulzProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://ww4.5movierulz.sbs"
    override var name = "5movierulz"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
    )

    override val mainPage = mainPageOf(
        "$mainUrl/category/featured/" to "Popular Movies",
        "$mainUrl/category/hollywood-movie-2023/" to "English",
        "$mainUrl/bollywood-movie-free/" to "Hindi",
        "$mainUrl/tamil-movie-free/" to "Tamil",
        "$mainUrl/telugu-movie/" to "Telugu",
        "$mainUrl/malayalam-movie-online/" to "Malayalam",
        "$mainUrl/category/bengali-movie/" to "Bengali",
        "$mainUrl/category/punjabi-movie/" to "Punjabi",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("#main .cont_display").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title =
            this.selectFirst("a")?.attr("title")?.trim()?.substringBefore("(") ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("#main .cont_display").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h2.entry-title")?.text()?.trim()?.substringBefore("(")
            ?: return null
        val poster = fixUrlNull(document.selectFirst(".entry-content img")?.attr("src"))
        val tags =
            document.select("div.entry-content > p:nth-child(5)").text().substringAfter("Genres:")
                .substringBefore("Country:").split(",").map { it }
        val yearRegex = Regex("""\d{4}""")
        val year = yearRegex.find(
            document.select("h2.entry-title").text()
        )?.groupValues?.getOrNull(0)?.toIntOrNull()
        val description = document.select("div.entry-content > p:nth-child(6)").text().trim()
        val actors =
            document.select("div.entry-content > p:nth-child(5)").text()
                .substringAfter("Starring by:")
                .substringBefore("Genres:").split(",").map { it }

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
        val sources = ArrayList<String>()
        app.get(data).document.select(".entry-content  p a[target=_blank]")
            .mapNotNull {
                if (it.attr("href").startsWith("https://down")) {
                    sources.add(
                        app.get(it.attr("href")).document.select(".entry-content iframe")
                            .attr("src")
                    )
                } else {
                    sources.add(it.attr("href"))
                }
            }
        sources.forEach { source ->
            val src = if (source.startsWith("https://stream")) source.replace(
                "/([a-z])/".toRegex(),
                "/e/"
            ) else source
            loadExtractor(
                src,
                "$mainUrl/",
                subtitleCallback,
                callback
            )
        }
        return true
    }
}

class Sbanh : StreamSB() {
    override var mainUrl = "https://sbanh.com"
}

class Ncdnstm : XStreamCdn() {
    override var mainUrl = "https://ncdnstm.xyz"
}

class StreamTapeAdblockUser : StreamTape() {
    override var mainUrl = "https://streamtapeadblockuser.homes"
}
