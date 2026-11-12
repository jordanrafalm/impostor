# ADR-0010: Local review prompt

## Status

Accepted.

## Decision

After one completed game recorded by this feature, show a centered review request on the home screen at the next app launch, provided no other startup dialog is present. If another startup dialog is shown, defer the review request until a later launch rather than displaying dialogs in succession. If Black Friday was previously cancelled and would otherwise recur, a qualifying review request takes its place for that launch; other startup dialogs retain priority. If the user postpones the review, require both seven days and one further completed game before asking again at a subsequent launch. Back and outside dismissal have the same effect as postponing.

Persist the count, postponement time, and whether the store was opened using the existing device-local preference store. Do not send this state to Firebase. A successful attempt to open the store permanently suppresses the prompt on that installation; the stores do not tell the app whether a review was actually submitted. An opening failure remains visible and can be retried.

The explicit review button opens the Google Play listing or the App Store write-review link. It does not submit a rating or use the stores' quota-limited in-app review prompts. State is local to the device and may be lost on uninstall.
