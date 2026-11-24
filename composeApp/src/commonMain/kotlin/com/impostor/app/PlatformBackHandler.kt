package com.impostor.app

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
