package com.impostor.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIColor
import platform.UIKit.UIRectEdgeAll
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { ImpostorApp() }.apply {
	view.backgroundColor = UIColor.clearColor
	edgesForExtendedLayout = UIRectEdgeAll
	extendedLayoutIncludesOpaqueBars = true
}