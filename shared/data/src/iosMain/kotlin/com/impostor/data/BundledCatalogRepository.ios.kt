package com.impostor.data

import com.impostor.domain.CatalogRepository
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
actual fun bundledCatalogRepository(): CatalogRepository {
    return localizedCatalogRepository(currentAppLocale()) { fileName ->
        runCatching {
            val path = NSBundle.mainBundle.pathForResource(fileName.removeSuffix(".json"), "json")
                ?: return@runCatching null
            val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return@runCatching null
            val bytes = data.bytes ?: return@runCatching null
            bytes.readBytes(data.length.toInt()).decodeToString()
        }.getOrNull()
    }
}