package com.darkdemon

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities


// credits: hexated
//
object SoraJioTVExtractor: SoraJioTVProvider() {

    suspend fun invokeNP(
        id: Int? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val document = app.get("$NPJioTV/play.php?id=$id").document
        val link =  "$NPJioTV/${document.selectFirst("source")?.attr("src")}"
        callback.invoke(
            ExtractorLink(
                this.name,
                "NPJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
    suspend fun invokeS(
        id: Int? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val document = app.get("$SJioTV/play.php?id=$id").document
        val link =  "$SJioTV/${document.selectFirst("source")?.attr("src")}"
        callback.invoke(
            ExtractorLink(
                this.name,
                "SJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }

    fun invokeGDL(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$GDLJioTV/autoq.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "GDLJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }

    fun invokeI(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$IJioTV/https://epic-austin.128-199-17-57.plesk.page/$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "IJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }

    fun invokeFS(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$FSJioTV/autoq.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "FSJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
    fun invokeFH(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$FHJioTV/autoq.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "FHJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
    fun invokeTS(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$TSJioTV/jtv/autoqtv.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "TSJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
    fun invokeBF(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$BFJioTV/autoq.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "BFJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
    fun invokeRPK(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ){
        val link = "$RPKJioTV/JIOTVx/autoq.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "RPKJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
}
