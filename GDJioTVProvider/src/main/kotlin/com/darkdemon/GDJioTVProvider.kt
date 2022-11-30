package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class GDJioTVProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://tv.googledrivelinks.com"
    override var name = "GDJioTV"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = false
    override val supportedTypes = setOf(
        TvType.Live,
    )

    override suspend fun getMainPage(
        page: Int, request: MainPageRequest
    ): HomePageResponse {

        val document = app.get(mainUrl).document
        val home = document.select(".row .card").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(
            HomePageList(
            name = request.name,
            home,
            isHorizontalImages = true
        ))
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.selectFirst("p.card-text")?.text()?.trim().toString()
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, title, TvType.Live) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get(mainUrl).document
        return document.select(".row .card:contains($query)").map {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document =
            app.get(mainUrl).document.selectFirst(".card:contains(${url.substringAfterLast("/")})")
        val title = document?.selectFirst("p.card-text")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.select("img").attr("data-src"))
        val href = fixUrl("$mainUrl/" + document.selectFirst("a")?.attr("href").toString())
        return newMovieLoadResponse(title, url, TvType.Live, href) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val link = "$mainUrl/${document.selectFirst("source")?.attr("src")}"
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
        return true
    }
}