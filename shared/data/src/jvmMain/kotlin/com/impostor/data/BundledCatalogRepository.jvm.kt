package com.impostor.data

import com.impostor.domain.CatalogRepository

actual fun bundledCatalogRepository(): CatalogRepository {
    val json = requireNotNull(object {}.javaClass.getResourceAsStream("/seed/prompts.json")) {
        "Bundled seed prompts.json is missing"
    }.bufferedReader().use { it.readText() }
    return SeedCatalogRepository(json)
}
