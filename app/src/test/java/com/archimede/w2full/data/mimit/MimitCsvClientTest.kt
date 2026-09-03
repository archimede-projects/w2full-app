package com.archimede.w2full.data.mimit

import java.io.IOException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MimitCsvClientTest {
    @Test
    fun downloadsAndParsesBothDatasetsFromLocalMockServer() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .addHeader("Content-Type", "text/csv; charset=utf-8")
                    .body(resourceText("mimit/anagrafica_sample.csv"))
                    .build(),
            )
            server.enqueue(
                MockResponse.Builder()
                    .addHeader("Content-Type", "text/csv; charset=utf-8")
                    .body(resourceText("mimit/prezzi_sample.csv"))
                    .build(),
            )
            server.start()

            val client = MimitCsvClient(
                httpClient = OkHttpClient(),
                stationsUrl = server.url("/stations.csv").toString(),
                pricesUrl = server.url("/prices.csv").toString(),
            )

            val stations = client.downloadStations()
            val prices = client.downloadPrices()

            assertEquals(3, stations.rows.size)
            assertEquals(3, prices.rows.size)
            assertEquals("/stations.csv", server.takeRequest().url.encodedPath)
            assertEquals("/prices.csv", server.takeRequest().url.encodedPath)
        }
    }

    @Test
    fun failsFastOnHttpErrorWithoutTryingToParseBody() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .code(503)
                    .body("temporarily unavailable")
                    .build(),
            )
            server.start()

            val client = MimitCsvClient(
                httpClient = OkHttpClient(),
                stationsUrl = server.url("/stations.csv").toString(),
                pricesUrl = server.url("/prices.csv").toString(),
            )

            assertThrows(IOException::class.java) {
                client.downloadStations()
            }
        }
    }

    private fun resourceText(path: String): String = requireNotNull(
        javaClass.classLoader?.getResource(path),
    ) { "Missing test resource $path" }.readText()
}
