package com.darkdemon

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

class SnehIPTVProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://snehiptv.netlify.app"
    override var name = "SnehIPTV"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = false
    override val supportedTypes = setOf(
        TvType.Live
    )

    data class IPTV(
        @JsonProperty("id") var id: String? = null,
        @JsonProperty("tvgLogo") var tvgLogo: String? = null,
        @JsonProperty("title") var title: String? = null,
        @JsonProperty("url") var url: String? = null,
        @JsonProperty("url1") var url1: String? = null
    )

    private suspend fun getScriptData(url: String): String {
        val html = app.get(url).document
        val script = html.select("script").last()?.attr("src")
        val doc = app.get("$mainUrl$script").text
        return doc.substringAfter("JSON.parse('").substringBefore("')},:")
    }

    override suspend fun getMainPage(
        page: Int, request: MainPageRequest
    ): HomePageResponse {

        val categories = listOf(
            "sonyliv",
            "voot",
            "sports",
            "entertainment",
            "movies",
            "news",
            "music",
            "kids",
            "infotainment",
            "lifestyle",
            "business",
            "educational",
            "devotional"

        )
        val items = ArrayList<HomePageList>()
        val scriptData = getScriptData(mainUrl)
        val response = parseJson<List<IPTV>>(scriptData)
        categories.forEach { cat ->
            val results: MutableList<SearchResponse> = mutableListOf()
            val filtered = response.filter { it.title?.lowercase()?.contains(cat) == true }
            filtered.forEach {
                val title = it.title?.replace(regex = "\\s\\[[A-Za-z]+]".toRegex(), "").toString()
                val posterUrl = it.tvgLogo.toString()
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

    override suspend fun search(query: String): List<SearchResponse>? {

        val scriptData = getScriptData(mainUrl)
        val response = parseJson<List<IPTV>>(scriptData)
        val searchResults =
            response.filter { it.title?.lowercase()?.contains(query.lowercase()) == true }
        return searchResults.map {
            val title = it.title?.replace(regex = "\\s\\[[A-Za-z]+]".toRegex(), "").toString()
            val posterUrl = it.tvgLogo.toString()
            newMovieSearchResponse(title, title, TvType.Live) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {

        val scriptData = getScriptData(mainUrl)
        val response = parseJson<List<IPTV>>(scriptData)
        val searchResults =
            response.filter { it.title?.contains(url.substringAfterLast("/")) == true }
        val title =
            searchResults[0].title?.replace(regex = "\\s\\[[A-Za-z]+]".toRegex(), "").toString()
        val posterUrl = searchResults[0].tvgLogo.toString()
        val href =
            if (searchResults[0].url.isNullOrEmpty()) searchResults[0].url1 else searchResults[0].url
        return newMovieLoadResponse(title, url, TvType.Movie, href) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val link = if (data.contains("sonyliv")) {
            app.get(data).document.selectFirst(".movie__credits a")?.attr("href").toString()
        } else if (data.contains("voot")) {
            app.get(data).document.selectFirst("source")?.attr("src").toString()
        } else {
            val html = app.get(data)
            html.url.substringBeforeLast("/") + "/${
                html.document.selectFirst("source")?.attr("src")
            }"
        }.toString()
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
