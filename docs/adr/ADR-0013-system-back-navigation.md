# ADR-0013: System back navigation

## Status

Accepted

## Context

The shared Compose app keeps the active route in `ImpostorApp`, while Android's system back gesture is handled by the activity. Without a Compose back callback, a gesture on a non-root screen finishes the activity instead of returning to the screen that opened it.

## Decision

- Keep route ownership and rendering in shared Compose code.
- Track the route history alongside the current route and save it with Compose saveable state.
- Handle Android system back through `androidx.activity.compose.BackHandler`; use the previous route for both the system gesture and in-app back/close controls.
- When restoring an older saved state without route history, fall back to Home from a non-root route.
- Leave system back unhandled at Home so Android retains the normal app-exit behavior.
- The iOS implementation does not install a system back callback.

## Consequences

Back navigation is consistent across Android gestures and in-app controls, including returning from Premium to the screen that opened it. Route history survives activity recreation and is cleared when a game finishes. The Android root remains dismissible through the platform's normal back behavior.
