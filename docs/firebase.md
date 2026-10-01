# Firebase setup

Firebase is an optional remote entitlement source. The bundled catalog and pass-and-play gameplay remain usable without a network connection.

## Supplied Firebase project

The supplied configuration identifies project `impostor-fca10`.

- Android application ID: `com.impostor.app`
- iOS bundle ID: `com.impostor.app`
- Android config: `composeApp/google-services.json`
- iOS config: `iosApp/GoogleService-Info.plist`

The configuration files are intentionally ignored by Git. Never print their contents or commit them to a public repository.

## Platform setup

1. Create or select the Firebase project matching the supplied platform config.
2. Register an Android app with application ID `com.impostor.app`.
3. Register an iOS app with bundle ID `com.impostor.app`.
4. Put the Android file at `composeApp/google-services.json`.
5. Add `iosApp/GoogleService-Info.plist` to the iOS app target in Xcode. Add Firebase iOS SDK products `FirebaseCore`, `FirebaseFirestore`, and `FirebaseAnalytics` through Swift Package Manager, then call `FirebaseApp.configure()` in the iOS host before the shared UI starts.
6. The Android host initializes through `com.google.gms.google-services`. GitLive uses the platform Firebase SDKs behind the shared `:shared:data` boundary.

The config files are ignored by Git. Do not paste their contents into logs, issue reports, or source files.

## Promo code migration

The subscription release no longer reads `promoCodes` and no premium access depends on Firestore. Keep existing documents only for one rollback release, then lock or delete the collection and remove its public-read rule.

## Legacy promo code contract

Create this Firestore collection and document shape in the Firebase Console. Do not create a code in Kotlin source.

1. Open **Firestore Database** in the Firebase Console and create a database in production mode.
2. Select **Start collection**, enter `promoCodes`, and create a document whose ID is the normalized promo code in uppercase, for example `SPRING_ALL`.
3. Add `enabled` as a Boolean, normally `true`.
4. Add `unlockAllCategories` as a Boolean. Set it to `true` for the production code that unlocks all 10 active categories.
5. Optionally add `categoryIds` as an array of strings for a category-specific entitlement. A valid document must have either `unlockAllCategories: true` or at least one category ID.
6. Optionally add `expiresAt` as a Firestore timestamp. The code is invalid at and after this timestamp.

The app reads the `promoCodes` collection once when the code repository first needs it. It validates the normalized document ID and fields locally. A successful fetch is cached in memory for the current app session, so cached codes continue to work when the device goes offline. Firebase failures are treated as unavailable and never block gameplay. Submitted codes are not persisted.

For the requested all-category unlock, create a document with ID `IMPOSTOR-ALL-2026` and fields:

```text
enabled: true
unlockAllCategories: true
categoryIds: []
```

The app maps `unlockAllCategories: true` to every active category from the bundled catalog. The document must be created in Firebase Console; the app deliberately does not contain a privileged Firestore write credential.

## Firestore security rules

The current adapter does not require authentication because promo-code documents are non-secret entitlements and the client only reads them. Paste these rules in **Firestore Database > Rules**, then publish:

```text
rules_version = '2';
service cloud.firestore {
	match /databases/{database}/documents {
		match /promoCodes/{code} {
			allow read: if true;
			allow write: if false;
		}
		match /{document=**} {
			allow read, write: if false;
		}
	}
}
```

Manage codes only through the Firebase Console or a trusted server/admin process. If the project later requires authenticated reads, add anonymous sign-in behind the data boundary and change `allow read` to `request.auth != null`; gameplay must still remain usable from the local cache after a successful fetch.

## Analytics

The legacy adapter may still contain mappings for historical events, but the production navigation no longer emits them. The subscription release emits:

- `premium_screen_opened`
- `subscription_product_loaded`
- `subscription_purchase_started`
- `subscription_purchase_result` with allowlisted result values
- `subscription_restore_result` with `active`, `none`, or `failed`
- `premium_entitlement_changed` with `active`, `expired`, `revoked`, or `locked`

Firebase Analytics is telemetry only and never decides entitlement. Never send receipts, transaction IDs, purchase tokens, product payloads, account identifiers, promo codes, or raw store errors.

## Validation

```bash
./gradlew :shared:domain:jvmTest :shared:data:jvmTest :composeApp:assembleDebug --no-daemon
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 --no-daemon
```