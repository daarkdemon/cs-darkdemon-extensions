package com.darkdemon

import android.util.Base64
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class EinthusanProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://einthusan.tv"
    override var name = "Einthusan"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override var sequentialMainPage = true
    override var sequentialMainPageDelay: Long = 25L
    override val supportedTypes = setOf(
        TvType.Movie,
    )

    data class PostJson(
        @JsonProperty("EJOutcomes") val EJOutcomes: String,
        @JsonProperty("NativeHLS") val NativeHLS: Boolean = false,
    )

    data class Response(
        @JsonProperty("Data") var Data: Data? = Data()
    )

    data class Data(
        @JsonProperty("EJLinks") var ejLinks: String? = null
    )

    data class VideoLink(
        @JsonProperty("MP4Link") var mp4Link: String? = null,
        @JsonProperty("HLSLink") var hlsLink: String? = null,
    )

    override val mainPage = mainPageOf(
        "$mainUrl/movie/results/?find=Popularity&lang=hindi&ptype=view&tp=tw&page=" to "Hindi Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=marathi&ptype=view&tp=tw&page=" to "Marathi Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=tamil&ptype=view&tp=tw&page=" to "Tamil Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=telugu&ptype=view&tp=tw&page=" to "Telugu Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=malayalam&ptype=view&tp=tw&page=" to "Malayalam Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=kannada&ptype=view&tp=tw&page=" to "Kannada Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=bengali&ptype=view&tp=tw&page=" to "Bengali Movies",
        "$mainUrl/movie/results/?find=Popularity&lang=punjabi&ptype=view&tp=tw&page=" to "Punjabi Movies"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("#UIMovieSummary > ul > li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title h3")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst(".title")?.attr("href").toString())
        val posterUrl = fixUrl(this.selectFirst(".block1 img")?.attr("src")!!)
        //println(posterUrl)
        val quality = getQualityFromString(if (this.select(".info i").hasClass("hd")) "HD" else "")
        val yearRegex = Regex("""(\d{4})""")
        val year = yearRegex.find(
            this.select(".info p:first-child").text()
        )?.groupValues?.getOrNull(1)?.toIntOrNull()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
            this.year = year
        }
    }

    //Search query [language, title] example : hindi, kantara
    override suspend fun search(query: String): List<SearchResponse> {
        val language = query.split(",")[0]
        val title = query.substringAfter(",")
        println("$mainUrl/movie/results/?lang=$language&query=$title")
        val document = app.get("$mainUrl/movie/results/?lang=$language&query=$query").document

        return document.select("#UIMovieSummary > ul > li").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val html = app.get(url)
        val document = html.document

        val title =
            document.selectFirst("#UIMovieSummary .block2 h3")?.text()?.trim() ?: return null
        val poster = fixUrl(
            document.selectFirst("#UIMovieSummary .block1 img")?.attr("src")!!
        )
        //println(poster)
        val tags = document.select(".average-rating label").map { it.text() }
        val yearRegex = Regex("""(\d{4})""")
        val year = yearRegex.find(
            document.select(".info p:first-child").text()
        )?.groupValues?.getOrNull(1)?.toIntOrNull()
        val description = document.select("p.synopsis").text()
        val actors =
            document.select(".professionals p").map { it.text() }
        val pageID = document.select("html").attr("data-pageid")
        val ejpingables = document.select("section #UIVideoPlayer").attr("data-ejpingables")
        println("$pageID\n$ejpingables")
        println(html.cookies)
        val href = getStreamLink(url, pageID, ejpingables, html.cookies)
        println("href: $href")
        return newMovieLoadResponse(title, url, TvType.Movie, href) {
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
        val urls = data.split(",")
        urls.forEach {
            if (!it.contains("m3u8")) {
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name + " mp4",
                        it,
                        "",
                        quality = Qualities.Unknown.value,
                    )
                )
            } else {
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name + " m3u8",
                        it,
                        "",
                        quality = Qualities.Unknown.value,
                        isM3u8 = true,
                    )
                )
            }
        }
        return true
    }

    private fun decode(encrypted_data: String): String {
        val string =
            encrypted_data.slice(0..9) + encrypted_data.last() + encrypted_data.slice(12..encrypted_data.length - 2)
        val decodedString = String(Base64.decode(string, Base64.DEFAULT))
        println(decodedString)
        val mp4Link = parseJson<VideoLink>(decodedString).mp4Link
        val m3u8Link = parseJson<VideoLink>(decodedString).hlsLink
        println("$mp4Link\n$m3u8Link")
        return "$mp4Link,$m3u8Link"
    }

    private suspend fun getStreamLink(
        url: String,
        pageId: String,
        ejpingables: String,
        cookies: Map<String, String>
    ): String {
        val link = url.replace("movie", "ajax/movie")
        val jsonObject = PostJson(ejpingables)
        val json = mapper.writeValueAsString(jsonObject)
        println(json)
        println(link)
        val data = mapOf(
            "xEvent" to "UIVideoPlayer.PingOutcome",
            "xJson" to json,
            "arcVersion" to "3",
            "appVersion" to "59",
            "gorilla.csrf.Token" to pageId
        )
        val response = app.post(
            url = link,
            data = data,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "Referer" to url,
            ),
            cookies = cookies,
        ).text
        println(response)
        val slink = try {
            parseJson<Response>(response).Data?.ejLinks.toString()
        } catch (e: Exception) {
            Log.d("Einthusan Provider", "rate limited")
        }
        println(slink)
        return decode(slink as String)
    }

    private fun fixUrl(url: String): String {
        if (url.isEmpty())
            return ""
        else if (url.startsWith("//img"))
            return "https:$url"
        else if (url.startsWith("/movie"))
            return "$mainUrl$url"
        return url
    }
}
