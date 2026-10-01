# iOS host integration

The required iOS bundle identifier is `com.impostor.app`.

This directory intentionally does not claim to contain a runnable Xcode project. There is no checked-in `iosApp.xcodeproj` yet. `iosApp.swift` is a minimal SwiftUI placeholder, not a built host for the Compose framework.

## Create the host in Xcode

1. Create a new iOS App project named `iosApp` in this directory, using SwiftUI and bundle identifier `com.impostor.app`.
2. Add the existing `iosApp.swift` as the app entry point. It initializes `FirebaseApp` and presents the shared `MainViewController()`.
3. Add a framework search/build phase that runs:

   ```bash
   ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
   ```

4. Link `composeApp/build/XCFrameworks/release/composeApp.xcframework`.
5. Import `ComposeApp` and set the root view controller to `MainViewController()` from `composeApp/src/iosMain/kotlin/com/impostor/app/MainViewController.kt`.
6. Set the target bundle identifier to `com.impostor.app`.
7. Add `GoogleService-Info.plist` to the target resources and add Firebase SPM products `FirebaseCore`, `FirebaseFirestore`, and `FirebaseAnalytics` from `https://github.com/firebase/firebase-ios-sdk`.

Until these steps are completed, the supported iOS verification is the KMP framework link task, not an Xcode app build.
