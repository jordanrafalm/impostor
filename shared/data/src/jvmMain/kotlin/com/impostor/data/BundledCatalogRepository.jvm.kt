package com.impostor.data

import com.impostor.domain.CatalogRepository

actual fun bundledCatalogRepository(): CatalogRepository {
    return localizedCatalogRepository(currentAppLocale()) { fileName ->
        object {}.javaClass.getResourceAsStream("/seed/$fileName")?.bufferedReader()?.use { it.readText() }
    }
}
