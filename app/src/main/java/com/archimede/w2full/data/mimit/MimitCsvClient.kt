package com.archimede.w2full.data.mimit

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request

class MimitCsvClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val parser: MimitCsvParser = MimitCsvParser(),
    private val stationsUrl: String = MimitEndpoints.STATIONS_URL,
    private val pricesUrl: String = MimitEndpoints.PRICES_URL,
) : MimitStationsDataSource {
    override fun downloadStations(): MimitDataset<MimitStation> =
        parser.parseStations(downloadText(stationsUrl))

    fun downloadPrices(): MimitDataset<MimitPrice> =
        parser.parsePrices(downloadText(pricesUrl))

    private fun downloadText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("MIMIT download failed with HTTP ${response.code} for $url")
            }
            return response.body.string()
        }
    }
}
