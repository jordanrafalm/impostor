# Store subscriptions

The app uses one monthly product on both stores:

```text
com.impostor.app.premium.monthly
```

## App Store Connect

1. Create an auto-renewable subscription group named `Impostor Premium`.
2. Add the product ID with localized title, description, price, tax category, and availability.
3. Create a Sandbox tester and test purchase, renewal, cancellation, refund/revocation, and restore after reinstall.
4. Submit the product with an app version when App Store Connect requires review.

## Google Play Console

1. Create subscription `com.impostor.app.premium.monthly`.
2. Add a monthly base plan and regional pricing. Offers are optional and map to the same entitlement.
3. Add license testers and publish the product to an internal testing track.
4. Verify package name `com.impostor.app` and test purchase, pending payment, already-owned restore, cancellation, and refund/revocation.

## Runtime boundary

Store verification is the only entitlement source. The shared cache is an availability optimization and never grants access by itself; it stores no receipt, token, transaction payload, or user identifier. Firebase Analytics does not participate in entitlement decisions.

`shared:data` now contains the real platform adapters. Android uses Google Play Billing `8.0.0`; `MainActivity` supplies its `Activity` through `initializePlatformStoreBilling`, while the adapter retains only the application context and uses the current activity for the purchase sheet. Product details are loaded before purchase, pending/cancelled/already-owned responses are mapped, purchased subscriptions are acknowledged before returning a `STORE` entitlement, and restore queries existing subscriptions. iOS uses a Swift StoreKit 2 host bridge because StoreKit 2's async Swift APIs are not exposed through this Kotlin/Native SDK surface. The bridge handles `Product.products`, `purchase()`, verified-only `Transaction.currentEntitlements`, `AppStore.sync`, `Transaction.updates`, `finish()`, and Apple's subscription-management URL. The Kotlin iOS adapter communicates with it through Foundation notifications.

Only verified, acknowledged/finished store transactions return `EntitlementSource.STORE`. Cached state remains an availability hint and cannot unlock categories by itself. Firebase, promo codes, and local flags are not entitlement sources.

## Android setup

1. Create the subscription and monthly base plan in Google Play Console using `com.impostor.app.premium.monthly`.
2. Publish the app to an internal, closed, or production testing track. The package name must remain `com.impostor.app`.
3. Add license testers in Play Console and install the build from the Play track. A sideloaded build normally cannot complete a real Play purchase.
4. Keep `composeApp/src/androidMain/kotlin/com/impostor/app/MainActivity.kt` as the host entry point. It initializes the billing factory before Compose creates `ImpostorApp`.
5. Test a fresh purchase, pending payment, user cancellation, already-owned purchase, explicit restore, renewal, cancellation, refund/revocation, and Play Store unavailability.

## iOS setup

1. Create an auto-renewable subscription group and the product `com.impostor.app.premium.monthly` in App Store Connect.
2. Configure the product's localized metadata, price, availability, and review status. Create a Sandbox tester.
3. Generate/open `iosApp/Impostor.xcodeproj` from `iosApp/project.yml` when the Xcode project is regenerated. StoreKit is an Apple system framework and does not require an SPM package entry.
4. Build the `Impostor` scheme after the Compose framework task has produced `ComposeApp.framework`. `iosApp.swift` installs the StoreKit 2 bridge before rendering Compose.
5. Test Sandbox purchase, pending/cancelled purchase, verified restore after reinstall, renewal, cancellation, expiration, refund/revocation, and subscription management.

The Xcode host build can require Firebase package resolution/network access even when the Kotlin framework has already compiled. If it stalls before Swift compiler output, resolve the existing Firebase packages in Xcode and retry the `Impostor` scheme; this is unrelated to StoreKit entitlement verification.

Required manual checks: fresh install, reinstall, explicit restore, renewal, cancellation, expiration, refund/revocation, airplane mode with a valid cached state, unavailable store, and large text/localized prices.

## Free games (1.0.2)

Fresh installs have three free game starts shared across all categories. All categories
are available during this allowance. Starting a valid game persists one use before
revealing roles; leaving the game or restarting the app does not refund it. Invalid
setups and opening categories do not consume uses. After the third start the current
game can finish, but starting another game or selecting a category requires Premium.
Verified subscribers can play without consuming free uses.

The existing platform preference keys are retained, so previously recorded games
continue to count after an update. This is a local per-install allowance, not an
account-wide or reinstall-resistant trial.
