package com.impostor.data

import android.content.Context
import com.impostor.domain.CatalogRepository

internal var catalogContext: Context? = null

fun initializeBundledCatalog(context: Context) {
    catalogContext = context.applicationContext
}

actual fun bundledCatalogRepository(): CatalogRepository {
    val context = requireNotNull(catalogContext) {
        "Bundled catalog must be initialized before use"
    }
    return localizedCatalogRepository(currentAppLocale()) { fileName ->
        runCatching {
            context.assets.open("seed/$fileName").bufferedReader().use { it.readText() }
        }.getOrNull()
    }
}
