# ADR-0002: Optional Firebase integration behind local-first boundaries

## Status

Accepted.

## Context

Impostor is a fully local pass-and-play game. The bundled catalog and an active round must work without a network connection, an account, or Firebase configuration. Firebase is useful only for privacy-safe product analytics and for distributing category unlock codes or other remote code data.

## Decision

Keep gameplay and the catalog offline-first. Firebase is optional and fail-safe:

- `shared:domain` owns `CodeRepository`, `AnalyticsService`, and the `ValidateCodeUseCase` contracts.
- `shared:data` owns cache-first code validation and the `RemoteCodeSource` / analytics platform seams.
- `composeApp` calls only the domain use case and analytics contract. Composables and ViewModels never import or call Firebase SDKs.
- Remote codes are fetched when the cache is empty, retained locally for the session, and reused when a later fetch fails. An unavailable remote source never prevents starting a local game.
- Analytics may record event names and coarse non-sensitive values such as validation outcome. It must never include player names, prompt text, assignments, code values, or tokens.
- Firebase Analytics is the intended analytics provider. Firebase Remote Config is the preferred provider for small code catalogs; Firestore is an alternative when codes need structured records or expiry metadata.

The current implementation uses GitLive Firestore and Analytics adapters behind `shared:data`. Android is configured through `composeApp/google-services.json`; iOS requires adding the supplied plist to the Xcode target and initializing FirebaseCore in the host. The app fetches `promoCodes` read-only, caches the result for the current session, and maps `unlockAllCategories: true` to all active bundled categories. Firebase failures remain fail-safe.

## Consequences

The app is immediately buildable and fully playable offline. Remote code distribution and analytics are active when the Firebase platform configuration is present. A Firestore administrator must create the `IMPOSTOR-ALL-2026` document described in `docs/firebase.md`; no write credential is shipped in the app.