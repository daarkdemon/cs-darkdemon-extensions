package com.darkdemon

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities


// credits: hexated
//
object SoraJioTVExtractor: SoraJioTVProvider() {

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

    suspend fun invokeTML(
        id: String? = null,
        category: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        if (category == 30) {
            val link = "$TMLJioTV/zee5/zeeapi.php?c=$id"
            callback.invoke(
                ExtractorLink(
                    this.name,
                    "TMLZee5",
                    link,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )

        } else if (category == 31) {
            val document = app.get("$TMLJioTV/sonyliv/channels/$id").document
            val link = document.select("source").attr("src")
            callback.invoke(
                ExtractorLink(
                    this.name,
                    "TMLSonyLiv",
                    link,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )

        } else {
            val link = "$TMLJioTV/autoq.php?c=$id"
            callback.invoke(
                ExtractorLink(
                    this.name,
                    "TMLJioTV",
                    link,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )
        }
    }

    fun invokeSW(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val link = "$SWJioTV/app/master.php?id=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "SWJioTV",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }

    fun invokeZL(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val link = "$ZLZee5/api.php?c=$id"
        callback.invoke(
            ExtractorLink(
                this.name,
                "ZLZee5",
                link,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
}
