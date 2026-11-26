# ADR-0014: Optional app update prompt

## Decision

Check for an update asynchronously at startup. Google Play's in-app update
availability API reports whether an update is available to the installed app
and account. On iOS, look up app ID `6811896797` in the current App Store
storefront, compare its published version with the running bundle's version,
and exclude listings requiring a newer OS. Invalid or unavailable information,
network errors and timeouts do not block play or show an update prompt.

On Home, a dismissible shared dialog offers to open the platform's store listing
or continue without updating. Back and outside dismissal also continue. Do not
show it again during the current app process; opening a listing does not prove an
update was installed. An opening failure remains visible and can be retried.

Do not interrupt other startup dialogs or notification permission requests.
If an update is available, defer the review prompt to a later launch, consistent
with ADR-0010. A bounded check prevents store access from indefinitely delaying
other prompts. Discard in-flight results when the app leaves the foreground.

## Limitations and verification

The iOS lookup is public listing metadata, not confirmation that an update is
available to a particular device or account; phased releases and propagation
can differ. Play availability depends on the installed build, account, track,
signature and Play services. Checks may be unavailable offline. Neither store
is used as an entitlement source.

Unit tests cover numeric versions and prompt blockers. Verify actual Google Play
availability with internal app sharing and App Store behavior with a released
build, including storefronts and OS compatibility. Also verify lifecycle,
accessibility, dismissal, store-opening failures and other startup prompts on
devices; framework linking alone does not establish those runtime behaviors.
