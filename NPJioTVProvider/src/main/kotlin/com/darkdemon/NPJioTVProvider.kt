package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class NPJioTVProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://nayeemparvez.chadasaniya.ml"
    override var name = "NPJioTV+"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = false
    override val supportedTypes = setOf(
        TvType.Live,
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val categories = listOf(
            "Sports",
            "Entertainment",
            "Movies",
            "News",
            "Music",
            "Kids",
            "Infotainment",
            "Lifestyle",
            "Business",
            "Devotional",
            "Educational",
            "JioDarshan",
            "Shopping",
        )
        val items = ArrayList<HomePageList>()
        val document = app.get(mainUrl).document
        categories.forEach { cat ->
            val results: MutableList<SearchResponse> = mutableListOf()
            document.select(".card-parent .card:contains($cat)").mapNotNull {
                val title = it.selectFirst("h5")?.text()?.trim() ?: return@mapNotNull
                val posterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
                results.add(
                    newMovieSearchResponse(title, title, TvType.Live) {
                        this.posterUrl = posterUrl
                    }
                )
            }
            items.add(
                HomePageList(
                    capitalizeString(cat),
                    results,
                    isHorizontalImages = true
                )
            )
        }
        return HomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get(mainUrl).document
         val elements = document.select(".card-parent .card h5:contains($query)").map { it.parent()
             ?.parent() }
        return elements.map {
            val title = it?.selectFirst("h5")?.text()?.trim().toString()
            val posterUrl = fixUrlNull(it?.selectFirst("img")?.attr("data-src"))
            newMovieSearchResponse(title, title, TvType.Live) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(mainUrl).document.selectFirst(".card:contains(${url.substringAfterLast("/")})")
        val title = url.substringAfterLast("/")
        val poster = fixUrlNull(document?.select("img")?.attr("data-src"))
        val href = fixUrl("$mainUrl/" + document?.selectFirst("a")?.attr("href").toString())
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
        val link =  "$mainUrl/${document.selectFirst("source")?.attr("src")}"
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