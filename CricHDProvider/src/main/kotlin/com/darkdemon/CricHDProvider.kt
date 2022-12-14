package com.darkdemon

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class CricHDProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://pc9.crichd.com"
    override var name = "CricHD"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.Live
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(mainUrl).document
        val pageSelectors = listOf(
            Pair("Leagues", ".CSSTableGenerator table tbody tr"),
            Pair("Channels", "#sidebar-right li"),
        )
        val pages = pageSelectors.apmap { (title, selector) ->
            val list = document.select(selector).mapNotNull {
                it.toSearchResult()
            }
            HomePageList(title, list)
        }
        return HomePageResponse(pages)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        if (this.selectFirst(".mobile-hide")?.text().isNullOrEmpty()) {
            val title =
                this.selectFirst("a")?.attr("title")?.substringBefore("Live")?.trim() ?: return null
            val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
            val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
            return newMovieSearchResponse(title, href, TvType.Live) {
                this.posterUrl = posterUrl
            }
        } else {
            val title = this.selectFirst(".gametitle")?.text()?.trim() ?: return null
            val href = fixUrl(this.selectFirst("td:nth-child(3) > a")?.attr("href").toString())
            return newMovieSearchResponse(title, href, TvType.Live)
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

        val title = document.selectFirst("h1.heading-separator")?.text()?.trim() ?: return null
        val episodes = document.select("div > div:nth-child(2) > table > tbody tr")
            .mapIndexedNotNull { index, it ->
                val name = it.selectFirst("td")?.text()?.trim()
                val href = it.select("a").attr("href")
                Episode(
                    href,
                    name,
                    season = 1,
                    episode = index,
                )
            }

        return newTvSeriesLoadResponse(
            title,
            url,
            TvType.TvSeries,
            episodes.asSequence().filterNot { it.name == "Channel Name" }
                .filterNot { it.data == "https://www.crichd.com/link.php" }
                .filter { it.data.isNotEmpty() && it.name!!.isNotEmpty() }.toList()
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(url = data, referer = "$mainUrl/").document
        val fidRegex = Regex("""fid="(.*)"; v_w""")
        document.select("table.mobile-hide > tbody:nth-child(1) a:not([target=_blank])")
            .mapNotNull {
                val html = app.get(it.attr("href"), referer = data)
                val fid = fidRegex.find(html.text)?.groupValues?.getOrNull(1).toString()
                val script = html.document.select("script").attr("src")
                val link =
                    app.get(fixUrl(script)).text.substringAfter("src=\"").substringBefore("'+fid")
                val sourceDoc = app.get("$link$fid", referer = "$link$fid").text
                val srcRegex = Regex("""[(sourceUrl: ')|(source: ')](https?.*?.)',""")
                val source =
                    srcRegex.find(sourceDoc)?.groupValues?.getOrNull(1).toString()
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        url = source,
                        referer = link.substringBefore("embed.php"),
                        quality = Qualities.Unknown.value,
                        isM3u8 = true,
                    )
                )
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
}
