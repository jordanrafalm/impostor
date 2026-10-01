# ADR-0004: Local persistence for the player list

## Status

Accepted.

## Context

The pass-and-play game needs to retain the local player names between launches. The names are device-local game setup data and must not be sent to Firebase or stored with passwords, prompts, assignments, or other game data.

## Decision

Keep the player list in the existing platform preferences boundary:

- `composeApp` exposes a small `expect`/`actual` `PlayerPreferences` API.
- Android stores the serialized names in `SharedPreferences`.
- iOS stores the serialized names in `NSUserDefaults`.
- Shared domain code owns the deterministic codec and rejects malformed data, empty names, duplicate names, and lists shorter than three players.
- Missing or invalid data loads the existing default three players.
- The UI writes the list immediately after every add, edit, or delete operation.

The serialized value contains only normalized display names. Player IDs are recreated locally on load, and no Firebase or gameplay data is persisted by this feature.

## Consequences

Player names survive app restarts on each device. Corrupt or outdated preference values fail safely to the defaults. The feature adds no dependency and keeps platform-specific code limited to the storage implementations.