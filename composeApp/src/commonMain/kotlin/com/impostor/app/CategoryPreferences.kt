package com.impostor.app

internal expect fun readSelectedCategoryId(): String?

internal expect fun writeSelectedCategoryId(categoryId: String)