# Firebase setup

Firebase provides optional remote prompt additions. The bundled catalog and pass-and-play gameplay remain usable without a network connection.

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
5. Add `iosApp/GoogleService-Info.plist` to the iOS app target in Xcode. Add Firebase iOS SDK products `FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore`, and `FirebaseAnalytics` through Swift Package Manager, then call `FirebaseApp.configure()` in the iOS host before the shared UI starts.
6. The Android host initializes through `com.google.gms.google-services`. GitLive uses the platform Firebase SDKs behind the shared `:shared:data` boundary.

The config files are ignored by Git. Do not paste their contents into logs, issue reports, or source files.

## Remote prompt catalog

The Polish and English bundled catalogs (`prompts-pl.json` and `prompts-en.json`) remain available offline and preserve identical category and prompt IDs. The English catalog contains translated prompt text and curated hints. An unavailable or invalid English bundle falls back to Polish. The data module contains a repository that can load a cached Firestore prompt snapshot and perform an explicit refresh, but the current app root uses `bundledCatalogRepository()` and does not wire that repository into startup. Therefore cached-first background refresh is a required integration, not current app behavior. When wired, it must load the selected bundle plus cached snapshot before gameplay, refresh asynchronously, keep the prior valid snapshot on failures, and use the latest valid active remote document for a repeated `(locale, document ID)` without replacing bundled prompts. The intended behavior is specified in [ADR-0006](adr/ADR-0006-firebase-prompt-catalog-sync.md).

Create a Firestore collection named `prompts`. Add one document per prompt using a stable document ID and these fields:

```text
categoryId: "animals"     # must match a bundled category ID
text: "Wydra"
hint: "Rzeka"
active: true
locale: "pl"              # optional; missing locale means Polish
```

The app only adds active documents with valid non-empty text and hint for a category present in the bundled catalog and matching the current app locale. Supported values are `pl`, `en` and `uk` (language tags with regions are accepted); unknown values are ignored. Untagged legacy documents are Polish and never appear in English mode. Use unique, stable Firestore document IDs; use distinct IDs for locale-specific variants. Publishing is read-only from the app: add or edit documents through Firebase Console or a trusted server/admin process. No Firebase write credential is included in the app.

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

Prompt and promo-code reads do not require authentication; player-name uploads require an authenticated create. Paste these rules in **Firestore Database > Rules**, then publish:

```text
rules_version = '2';
service cloud.firestore {
	match /databases/{database}/documents {
		match /prompts/{promptId} {
			allow read: if true;
			allow write: if false;
		}
		match /promoCodes/{code} {
			allow read: if true;
			allow write: if false;
		}
		match /playerNameUploads/{uploadId} {
			allow create: if request.auth != null
				&& request.resource.data.keys().hasOnly(['names', 'createdAt'])
				&& request.resource.data.keys().hasAll(['names', 'createdAt'])
				&& request.resource.data.names is list
				&& request.resource.data.names.size() >= 3
				&& request.resource.data.createdAt == request.time;
			allow read, update, delete: if false;
		}
		match /{document=**} {
			allow read, write: if false;
		}
	}
}
```

Manage codes only through the Firebase Console or a trusted server/admin process. If other reads later require authentication, update only those collection rules; anonymous sign-in is already used behind the data boundary for player-name uploads. Gameplay must remain usable from the local cache after a successful prompt fetch.

## Analytics

The legacy adapter may still contain mappings for historical events, but the production navigation no longer emits them. The subscription release emits:

- `premium_screen_opened`
- `subscription_product_loaded`
- `subscription_purchase_started`
- `subscription_purchase_result` with allowlisted result values
- `subscription_restore_result` with `active`, `none`, or `failed`
- `premium_entitlement_changed` with `active`, `expired`, `revoked`, or `locked`

Firebase Analytics is telemetry only and never decides entitlement. Never send receipts, transaction IDs, purchase tokens, product payloads, account identifiers, promo codes, or raw store errors.

## Player-name uploads

At each valid game start, the app automatically attempts to upload the entered player names to the `playerNameUploads` collection. There is no separate upload confirmation. This is separate from local pass-and-play gameplay: an unavailable network or failed upload never prevents the game from starting. A failed attempt is shown as a brief, non-blocking in-app notice.

Enable **Authentication > Sign-in method > Anonymous** in the Firebase Console for the configured project. The shared data-layer adapter signs in anonymously before writing when there is no current Firebase user. No Firebase SDK call is made from the UI or domain layer.

Each document uses an automatically generated Firestore document ID and exactly these fields:

```text
names: ["Ala", "Bartek", "Celina"]  # list of entered player names
createdAt: server timestamp        # assigned by Firestore
```

The upload contains no prompt, category, role, game/session ID, or device identifier. The authenticated client can create a document only; client reads, updates, and deletes are denied. The app does not expose uploaded records; the Firebase project owner can review them in the Firebase Console or through a trusted administrative process. Records have no automatic expiry and are retained indefinitely, as requested. The project owner can remove records administratively if needed.

## Validation

```bash
./gradlew :shared:domain:jvmTest :shared:data:jvmTest :composeApp:assembleDebug --no-daemon
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 --no-daemon
```