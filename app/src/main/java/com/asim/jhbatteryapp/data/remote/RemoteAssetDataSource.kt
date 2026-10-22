package com.asim.jhbatteryapp.data.remote

import com.asim.jhbatteryapp.core.Utils.AssetConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fetches the raw manifest JSON string from the configured Dropbox URL.
 * Uses a shared OkHttpClient with sensible timeouts.
 * No authentication — Dropbox public share links are used.
 */
class RemoteAssetDataSource(
    private val client: OkHttpClient = defaultClient
) {

    /**
     * Downloads and returns the manifest JSON string.
     * @throws IOException on network failure or non-200 response.
     */
    @Throws(IOException::class)
    fun fetchManifestJson(): String {
        val request = Request.Builder()
            .url(AssetConfig.MANIFEST_URL.replace("dl=0", "dl=1"))
            .header("Cache-Control", "no-cache")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Manifest fetch failed: HTTP ${response.code} for ${AssetConfig.MANIFEST_URL}")
        }
        return response.body?.string()
            ?: throw IOException("Manifest response body was empty")
    }

    companion object {
        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
