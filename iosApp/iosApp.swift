import SwiftUI
import ComposeApp
import FirebaseCore
import Foundation
import StoreKit
import UIKit

@main
struct iosApp: App {
    init() {
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        StoreKitBridge.install()
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea(.all)
        }
    }
}

@MainActor
private final class StoreKitBridge {
    private static let requestName = Notification.Name("com.impostor.storekit.request")
    private static let responseName = Notification.Name("com.impostor.storekit.response")
    private static let entitlementUpdateName = Notification.Name("com.impostor.storekit.entitlementUpdate")
    private static let monthlyProductId = "com.impostor.app.premium.monthly"
    private static let yearlyProductId = "com.impostor.app.premium.yearly"
    private static let productIds: Set<String> = [monthlyProductId, yearlyProductId]
    private static var shared: StoreKitBridge?

    private var observer: NSObjectProtocol?
    private var updatesTask: Task<Void, Never>?

    static func install() {
        shared = StoreKitBridge()
    }

    private init() {
        observer = NotificationCenter.default.addObserver(
            forName: Self.requestName,
            object: nil,
            queue: .main,
        ) { [weak self] notification in
            Task { @MainActor in
                await self?.handle(notification)
            }
        }
        updatesTask = Task {
            for await result in StoreKit.Transaction.updates {
                await self.handle(transactionResult: result)
            }
        }
    }

    deinit {
        if let observer {
            NotificationCenter.default.removeObserver(observer)
        }
        updatesTask?.cancel()
    }

    private func handle(_ notification: Notification) async {
        guard
            let values = notification.userInfo,
            let requestId = values["requestId"] as? String,
            let action = values["action"] as? String
        else { return }
        let requestedProductId = values["productId"] as? String

        do {
            switch action {
            case "load":
                guard let productId = requestedProductId, Self.productIds.contains(productId) else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                let loadedProducts = try await Product.products(for: [productId])
                guard let product = loadedProducts.first(where: { $0.id == productId }) else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                var payload: [String: Any] = [
                    "status": "success",
                    "productId": product.id,
                    "title": product.displayName,
                    "price": product.displayPrice,
                    "period": periodLabel(product.subscription?.subscriptionPeriod),
                ]
                if let subscription = product.subscription,
                   subscription.introductoryOffer == nil,
                   subscription.subscriptionPeriod.value == 1,
                   ((productId == Self.monthlyProductId && subscription.subscriptionPeriod.unit == .month) ||
                    (productId == Self.yearlyProductId && subscription.subscriptionPeriod.unit == .year)),
                   !product.price.isNaN, product.price > 0,
                   !product.priceFormatStyle.currencyCode.isEmpty {
                    payload["comparisonPrice"] = NSDecimalNumber(decimal: product.price)
                    payload["comparisonCurrency"] = product.priceFormatStyle.currencyCode
                }
                respond(requestId, payload)
            case "purchase":
                guard let productId = requestedProductId, Self.productIds.contains(productId) else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                let refreshedProducts = try await Product.products(for: [productId])
                let product = refreshedProducts.first(where: { $0.id == productId })
                guard let product, product.id == productId else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                switch try await product.purchase() {
                case .success(let verification):
                    await respondVerified(requestId, verification, expectedProductId: productId)
                case .userCancelled:
                    respond(requestId, ["status": "error", "error": "cancelled"])
                case .pending:
                    respond(requestId, ["status": "error", "error": "pending"])
                @unknown default:
                    respond(requestId, ["status": "error", "error": "failed"])
                }
            case "restore":
                try await AppStore.sync()
                respond(requestId, await currentStoreEntitlement())
            case "reconcile":
                respond(requestId, await currentStoreEntitlement())
            case "monthlyPriceNotice":
                respond(requestId, try await monthlyPriceNotice())
            case "manage":
                guard let url = URL(string: "https://apps.apple.com/account/subscriptions") else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                UIApplication.shared.open(url) { opened in
                    self.respond(requestId, ["status": opened ? "success" : "error", "error": "unavailable"])
                }
            default:
                respond(requestId, ["status": "error", "error": "failed"])
            }
        } catch {
            respond(requestId, ["status": "error", "error": "store_unavailable"])
        }
    }

    private func handle(transactionResult: VerificationResult<StoreKit.Transaction>) async {
        guard case .verified(let transaction) = transactionResult, Self.productIds.contains(transaction.productID) else {
            return
        }
        NotificationCenter.default.post(
            name: Self.entitlementUpdateName,
            object: nil,
            userInfo: entitlementPayload(for: transaction),
        )
        await transaction.finish()
    }

    private func respondVerified(
        _ requestId: String,
        _ verification: VerificationResult<StoreKit.Transaction>,
        expectedProductId: String
    ) async {
        guard case .verified(let transaction) = verification else {
            respond(requestId, ["status": "error", "error": "unverified"])
            return
        }
        guard transaction.productID == expectedProductId else {
            respond(requestId, ["status": "error", "error": "failed"])
            return
        }
        await transaction.finish()
        respond(requestId, entitlementPayload(for: transaction))
    }

    private func currentStoreEntitlement() async -> [String: Any] {
        var fallbackStatus = "locked"
        for await result in StoreKit.Transaction.currentEntitlements {
            guard case .verified(let transaction) = result, Self.productIds.contains(transaction.productID) else {
                continue
            }
            let payload = entitlementPayload(for: transaction)
            if payload["status"] as? String == "active" {
                return payload
            }
            fallbackStatus = payload["status"] as? String ?? fallbackStatus
        }
        return ["status": fallbackStatus]
    }

    private func monthlyPriceNotice() async throws -> [String: Any] {
        // The offer API is required to rule out discounted next periods.
        guard #available(iOS 18.0, *) else { return ["status": "unknown"] }
        let products = try await Product.products(for: [Self.monthlyProductId])
        guard products.count == 1, let product = products.first,
              product.id == Self.monthlyProductId,
              let subscription = product.subscription,
              subscription.subscriptionPeriod.unit == .month,
              subscription.subscriptionPeriod.value == 1 else {
            return ["status": "unknown"]
        }
        let statuses = try await subscription.status
        if statuses.isEmpty { return ["status": "ineligible"] }
        // Do not guess which subscription is authoritative if there is more than one.
        guard statuses.count == 1, let status = statuses.first else { return ["status": "unknown"] }
        guard case .verified(let transaction) = status.transaction,
              case .verified(let renewal) = status.renewalInfo else {
            return ["status": "unknown"]
        }
        guard renewal.originalTransactionID == transaction.originalID,
              let expiration = transaction.expirationDate else {
            return ["status": "unknown"]
        }
        guard status.state == .subscribed,
              transaction.productID == Self.monthlyProductId,
              transaction.revocationDate == nil,
              expiration > Date(),
              renewal.currentProductID == Self.monthlyProductId,
              renewal.willAutoRenew,
              renewal.offer == nil else {
            return ["status": "ineligible"]
        }
        guard let nextProduct = renewal.autoRenewPreference else { return ["status": "unknown"] }
        guard nextProduct == Self.monthlyProductId else { return ["status": "ineligible"] }
        let currentCurrency = product.priceFormatStyle.currencyCode
        guard let renewalCurrency = renewal.currency?.identifier,
              !currentCurrency.isEmpty, renewalCurrency == currentCurrency,
              let renewalPrice = renewal.renewalPrice,
              !renewalPrice.isNaN, !product.price.isNaN,
              renewalPrice >= 0, product.price > 0 else {
            return ["status": "unknown"]
        }
        guard renewalPrice < product.price else { return ["status": "ineligible"] }
        let formattedRenewalPrice = renewalPrice.formatted(product.priceFormatStyle)
        guard !formattedRenewalPrice.isEmpty, !product.displayPrice.isEmpty else {
            return ["status": "unknown"]
        }
        return [
            "status": "eligible",
            "renewalPrice": formattedRenewalPrice,
            "currentPrice": product.displayPrice,
        ]
    }

    private func entitlementPayload(for transaction: StoreKit.Transaction) -> [String: Any] {
        if transaction.revocationDate != nil {
            return ["status": "revoked"]
        }
        if let expirationDate = transaction.expirationDate, expirationDate <= Date() {
            return ["status": "expired"]
        }
        var payload: [String: Any] = ["status": "active"]
        if let expirationDate = transaction.expirationDate {
            payload["expiresAtMillis"] = Int64(expirationDate.timeIntervalSince1970 * 1000)
        }
        return payload
    }

    private func periodLabel(_ period: Product.SubscriptionPeriod?) -> String {
        guard let period else { return "1 month" }
        switch period.unit {
        case .day: return "\(period.value) day"
        case .week: return "\(period.value) week"
        case .month: return "\(period.value) month"
        case .year: return "\(period.value) year"
        @unknown default: return "\(period.value) period"
        }
    }

    private func respond(_ requestId: String, _ values: [String: Any]) {
        var payload = values
        payload["requestId"] = requestId
        NotificationCenter.default.post(name: Self.responseName, object: nil, userInfo: payload)
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        #if DEBUG
        let debugUnlockCategories = true
        #else
        let debugUnlockCategories = false
        #endif
        let viewController = MainViewControllerKt.MainViewController(debugUnlockCategories: debugUnlockCategories)
        viewController.modalPresentationStyle = .fullScreen
        viewController.additionalSafeAreaInsets = .zero
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
