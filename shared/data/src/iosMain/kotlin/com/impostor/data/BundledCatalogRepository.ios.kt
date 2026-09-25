package com.impostor.data

import com.impostor.domain.CatalogRepository
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
actual fun bundledCatalogRepository(): CatalogRepository {
    val path = NSBundle.mainBundle.pathForResource("prompts", "json")
        ?: error("Bundled seed prompts.json is missing")
    val data = NSFileManager.defaultManager.contentsAtPath(path)
        ?: error("Unable to read bundled seed")
    val bytes = data.bytes ?: error("Bundled seed has no bytes")
    val json = bytes.readBytes(data.length.toInt()).decodeToString()
    return SeedCatalogRepository(json)
}