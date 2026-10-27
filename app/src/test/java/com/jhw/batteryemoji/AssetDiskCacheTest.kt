package com.jhw.batteryemoji

import com.jhw.batteryemoji.data.datasource.AssetDiskCache
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AssetDiskCacheTest {

    private lateinit var dir: File
    private lateinit var cache: AssetDiskCache

    private val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()

    private fun asset(id: String = "sticker 1/a", url: String = "https://x/a.svg?dl=0", version: Int = 0) =
        RemoteAssetItem(
            id = id, name = id, category = "hot", type = "sticker", format = "svg",
            downloadUrl = url, version = version
        )

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("asset-cache").toFile()
        cache = AssetDiskCache(dir)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun cachedAssetIsReusedForSameManifestEntry() {
        assertNull(cache.get(asset()))
        val written = cache.put(asset(), svg)
        assertNotNull(written)
        // A fresh but equal manifest entry (e.g. after relaunch) hits the same file
        assertEquals(written, cache.get(asset()))
    }

    @Test
    fun fileNameIsStableAndSafe() {
        val name = cache.fileName(asset())
        assertEquals(name, cache.fileName(asset()))
        assertFalse(name.contains('/'))
        assertFalse(name.contains(' '))
        assertTrue(name.endsWith(".svg"))
        // Ids that sanitize to the same text still get different files
        assertNotEquals(cache.fileName(asset(id = "a b")), cache.fileName(asset(id = "a_b")))
    }

    @Test
    fun changedEntryMissesAndReplacesOldVersion() {
        cache.put(asset(), svg)
        assertNull(cache.get(asset(version = 2)))
        assertNull(cache.get(asset(url = "https://x/new.svg")))

        cache.put(asset(version = 2), svg)
        assertNotNull(cache.get(asset(version = 2)))
        // The old version of the same id was deleted
        assertNull(cache.get(asset()))
        assertEquals(1, dir.listFiles()!!.size)
    }

    @Test
    fun retainOnlyKeepsUnchangedAndDropsOutdated() {
        cache.put(asset(id = "keep"), svg)
        cache.put(asset(id = "changed"), svg)
        cache.put(asset(id = "removed"), svg)

        cache.retainOnly(listOf(asset(id = "keep"), asset(id = "changed", version = 1)))

        assertNotNull(cache.get(asset(id = "keep")))
        assertNull(cache.get(asset(id = "changed")))
        assertNull(cache.get(asset(id = "removed")))
    }

    @Test
    fun newManifestVersionInvalidatesCachedFiles() {
        var manifestVersion = 6
        val versioned = AssetDiskCache(dir, generation = { manifestVersion })
        versioned.put(asset(), svg)
        assertNotNull(versioned.get(asset()))

        manifestVersion = 7
        // Same entry, new manifestVersion: not served from cache, so it is downloaded again
        assertNull(versioned.get(asset()))
        versioned.retainOnly(listOf(asset()))
        assertEquals(0, dir.listFiles()!!.size)

        versioned.put(asset(), svg)
        assertNotNull(versioned.get(asset()))
    }

    @Test
    fun trimsLeastRecentlyUsedOverLimit() {
        val small = AssetDiskCache(dir, maxBytes = svg.size * 2L)
        small.put(asset(id = "a"), svg)
        File(dir, small.fileName(asset(id = "a"))).setLastModified(1_000)
        small.put(asset(id = "b"), svg)
        small.put(asset(id = "c"), svg)

        assertNull(small.get(asset(id = "a")))
        assertNotNull(small.get(asset(id = "b")))
        assertNotNull(small.get(asset(id = "c")))
    }
}
