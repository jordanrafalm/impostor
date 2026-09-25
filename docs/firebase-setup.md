# Optional Firebase setup

Firebase is not required for the local pass-and-play game. Configure it only when enabling Firebase-backed features in your own environment.

## Android

1. Create a Firebase project and register an Android app using your application ID.
2. Download `google-services.json`.
3. Place it at `composeApp/google-services.json`.
4. Rebuild the Android app.

The Gradle build applies the Google Services plugin only when this file is present.

## iOS

1. Register an iOS app in the same Firebase project.
2. Download `GoogleService-Info.plist`.
3. Place it at `iosApp/GoogleService-Info.plist`.
4. Regenerate the Xcode project with `xcodegen generate` in `iosApp`.
5. Build the app in Xcode.

The iOS host starts without this file; Firebase initialization is skipped. Do not use production configuration files in public repositories.

## Security

- Never commit Firebase configuration, API keys, service-account files, receipts, purchase tokens, or offer codes.
- Restrict Firestore rules to the minimum required access.
- Treat analytics as optional telemetry; never send player names, prompt text, purchase tokens, or raw errors.
