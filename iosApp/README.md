# iOS host

The iOS host is generated from `project.yml` with XcodeGen. The generated `.xcodeproj` is intentionally not committed.

## Prerequisites

- Xcode 16 or newer
- XcodeGen
- An Apple signing team and bundle identifier configured for your account

## Build

```bash
./gradlew :composeApp:assembleComposeAppReleaseXCFramework
cd iosApp
xcodegen generate
open Impostor.xcodeproj
```

In Xcode, select your signing team and a bundle identifier, then run the `Impostor` scheme.

To enable Firebase, follow [../docs/firebase-setup.md](../docs/firebase-setup.md) before generating the Xcode project.
