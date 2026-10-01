# Build notes

The Android release build uses Kotlin 2.2.21 and AndroidX Lifecycle 2.9.4. The
`NullSafeMutableLiveData` bundled with Lifecycle 2.9.4 and Compose's
`FrequentlyChangingValue`, and `RememberInComposition` detectors currently crash under
this Kotlin UAST with an `IncompatibleClassChangeError`. These named detectors are
disabled for `composeApp` only; the rest of Android Lint remains enabled.

This is a tooling workaround, not an application warning suppression. Re-enable the
detector when a compatible Lifecycle/Android Gradle Plugin combination is adopted.