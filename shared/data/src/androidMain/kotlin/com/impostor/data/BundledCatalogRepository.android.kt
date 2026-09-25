package com.impostor.data

import android.content.Context
import com.impostor.domain.CatalogRepository

private var catalogContext: Context? = null

fun initializeBundledCatalog(context: Context) {
    catalogContext = context.applicationContext
}

actual fun bundledCatalogRepository(): CatalogRepository {
    val json = requireNotNull(catalogContext) {
        "Bundled catalog must be initialized before use"
    }.assets.open("seed/prompts.json").bufferedReader().use { it.readText() }
    return SeedCatalogRepository(json)
}
