# iOS host

The repository currently contains the shared Compose iOS framework entry point at
`composeApp/src/iosMain/kotlin/com/impostor/app/MainViewController.kt`. It exposes
`MainViewController()` using `ComposeUIViewController { ImpostorApp() }`.

There is no `iosApp` Xcode project in this checkout, so an installable iOS application
cannot be built or launched here. The supported host-side validation is the simulator
framework link task on Apple Silicon:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

An Xcode host can embed the resulting `ComposeApp.framework` and call
`MainViewController()` from its scene or application delegate when that host project is
added separately.