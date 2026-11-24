# ADR-0012: iOS monthly renewal-price modal

## Status

Implemented: typed eligibility boundary, verified iOS StoreKit proof query, shared Home modal, localized copy, persistent acknowledgement and prompt arbitration. Compose tests, Android debug assembly and iOS simulator framework linking pass. StoreKit sandbox and visual runtime verification remain pending.

## Context

The approved feature is a one-time Home modal for iOS/App Store monthly subscribers whose **next renewal** is cheaper than the currently displayed monthly product price. Android, annual, inactive, revoked and unverified subscribers are excluded. This is not a toast, notification, Firebase-backed eligibility feature or customer communication from App Store Connect. Firebase Analytics may record privacy-safe display and acknowledgement events only. Do not submit or publish products.

`Entitlement` currently carries only state, source and expiry, not a plan or renewal price. `SubscriptionRepository` and the iOS notification adapter bridge to `StoreKitBridge` in `iosApp/iosApp.swift`, which currently returns verified current-entitlement status and expiry only. ADR-0011 covers the separate monthly and yearly plans; no existing ADR or plan covers this modal. Home already arbitrates Black Friday, Sex intro and review prompts; ADR-0010 defers review when another startup dialog is shown.

## Decision

- **Store proof:** Query the monthly product `com.impostor.app.premium.monthly` and its `Product.SubscriptionInfo.Status` on iOS. Only a matching, unambiguous active monthly status with both a verified, non-revoked, unexpired transaction and verified renewal info qualifies. Confirm renewal info identifies the current and next product as monthly and `willAutoRenew` is true. Annual (`com.impostor.app.premium.yearly`), switches to another product, cancelled renewal, expired, revoked or ambiguous/multiple conflicting statuses do not qualify. An existing `ACTIVE/STORE` entitlement or cached entitlement alone is insufficient.
- **Price proof:** Compare the verified `renewalPrice` and the freshly fetched matching monthly `Product.price` as exact decimal values in the same explicitly verified currency; require renewal price **strictly less**. Missing values, incompatible or unknown currencies, unavailable APIs/product, verification or network errors and inconsistent results mean **unknown/no modal**, never a success-shaped fallback. Use `Product.displayPrice` for the current price and format the renewal price using the product's `priceFormatStyle` only after verifying compatible currency; never parse formatted strings or hardcode customer prices. Handle SDK/OS availability explicitly, including `currencyCode` where required; if trustworthy comparison or formatting is unavailable, suppress the modal.
- **OS availability:** `RenewalInfo.offer`, needed to rule out discounted next-period offers, is available only on iOS 18+. The app's deployment target is iOS 17.2, so the query returns unknown and suppresses this notice on earlier OS versions. Do not use a weaker fallback that could mistake a temporary offer for a preserved price.
- **Meaning:** Apple says `renewalPrice` includes any next-period offer discount. A lower next price alone does not establish a permanent grandfathered rate: copy must describe the verified next renewal and current monthly price, not promise that the price remains unchanged forever or assert why it is lower.
- **Boundaries:** Keep StoreKit verification, decimal/currency comparison and localized formatting in the Swift bridge. Extend the existing request-ID-correlated iOS notification/billing boundary with a separate typed eligibility result (`eligible` with formatted prices, `ineligible`, or `unknown`); preserve unknown across the shared data/domain contract. Other platforms return non-eligible/unsupported and never show the modal. Keep this result separate from entitlement and Premium unlocking, which remain unchanged. Do not persist eligibility or store transaction payloads, and do not send them to analytics.
- **Lifecycle and sequencing:** Request fresh proof asynchronously at startup/foreground and after relevant verified StoreKit changes. Invalidate previous proof on background, account/storefront or subscription changes and failed refresh; discard out-of-order results. Show only at Home while foregrounded and still eligible, with the most recently verified snapshot for that foreground session. Recheck before presenting after another dialog; retry on a subsequent Home/foreground opportunity if proof is unavailable. Loading or failure must not block Home, gameplay or other prompts indefinitely.
- **Home UI and persistence:** In shared Compose, render an accessible, responsive in-app dialog matching the supplied screenshot's copy and layout, with localized strings and store-formatted prices. Its explicit Polish action is **„Super, dzięki”**. Only this action acknowledges it; back/outside dismissal must not silently record acknowledgement. Persist a dedicated acknowledgement boolean via common `PreferenceStore`/`PreferenceKeys`; do not store it merely because eligibility was checked or the dialog was shown. Suppress future displays after acknowledgement across restarts. This is once per local installation, not per Apple account or across devices; uninstall/data clearing may reset it.
- **Analytics:** Emit `monthly_price_notice_shown` when the dialog is presented and `monthly_price_notice_acknowledged` only after the explicit CTA. Do not include renewal/current prices, product IDs, subscriber identifiers, transaction data or eligibility proof.
- **Prompt arbitration:** Never stack dialogs. Preserve Black Friday and Sex intro precedence; show the eligible price modal before review when the existing blockers are gone. Treat it as another startup dialog for ADR-0010: defer review to a later launch if this modal is shown, never show review immediately after it. If review or a system permission prompt is already shown/in flight when proof arrives, do not interrupt it or show the price modal in succession with review; defer to a later eligible Home opportunity/launch. Coordinate the existing reminder permission request so it cannot overlap this modal. Proof loading must not indefinitely postpone review or permission requests.

## Options and consequences

- **Verified StoreKit status and live price comparison:** selected; precise to the next renewal, localized and private, but may suppress eligible users when StoreKit cannot prove a result.
- **Global subscriber list, purchase date or local entitlement/cache:** rejected; these cannot establish current individual renewal price and introduce privacy or stale-state risks.
- **Hardcoded old/new price, toast, push or Firebase-backed eligibility:** rejected; prices vary by storefront and these do not meet the approved in-app behavior. Firebase Analytics is used only for the privacy-safe events above.

There is an unavoidable interval between StoreKit's snapshot and display; refresh/invalidation minimizes but cannot eliminate later App Store price or subscription changes. No change to subscription purchase, entitlement or store configuration is implied.

## Follow-ups and test plan

1. Automated coverage now verifies the typed repository boundary, unsupported/unknown behavior, persistent explicit acknowledgement and display gating. Keep StoreKit transaction verification tests in the platform boundary.
2. Still test restart persistence, dismissal without acknowledgement, Home-only display, blockers and ADR-0010 review deferral, permission sequencing, late/out-of-order results, background/foreground invalidation and a price/eligibility change before presentation. Check screen reader, large text and small layouts against the reference.
3. Compose tests, Android debug assembly and iOS simulator framework linking passed for this implementation. A full iOS host build, StoreKit Sandbox verification and visual runtime checks remain outstanding. Use StoreKit test configuration and App Store Sandbox to verify actual monthly status, renewal price/currency/formatting, annual, renewal cancellation, plan changes and revocation; do not claim these passed.

## Flow

```mermaid
sequenceDiagram
    participant UI as Home (Compose)
    participant Repo as Subscription boundary
    participant SK as iOS StoreKit bridge
    UI->>Repo: Request fresh eligibility
    Repo->>SK: Query monthly product and status
    SK->>SK: Verify transaction + renewal; compare same-currency decimals
    SK-->>Repo: Eligible with localized prices / ineligible / unknown
    Repo-->>UI: Typed result
    UI->>UI: At Home, foreground, undismissed, no competing prompt
    UI->>UI: Show modal only for current eligible proof
    UI->>UI: “Super, dzięki” persists acknowledgement
```

Sources: Apple [`Status`](https://developer.apple.com/documentation/storekit/product/subscriptioninfo/status-swift.struct), [`renewalPrice`](https://developer.apple.com/documentation/storekit/product/subscriptioninfo/renewalinfo/renewalprice), [`currency`](https://developer.apple.com/documentation/storekit/product/subscriptioninfo/renewalinfo/currency), [`autoRenewPreference`](https://developer.apple.com/documentation/storekit/product/subscriptioninfo/renewalinfo/autorenewpreference), and [`priceFormatStyle`](https://developer.apple.com/documentation/storekit/product/priceformatstyle).
