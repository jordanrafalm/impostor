import SwiftUI
import ComposeApp
import FirebaseCore
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
    private static let productId = "com.impostor.app.premium.monthly"
    private static var shared: StoreKitBridge?

    private var product: Product?
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

        do {
            switch action {
            case "load":
                let products = try await Product.products(for: [Self.productId])
                guard let product = products.first else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                self.product = product
                respond(requestId, [
                    "status": "success",
                    "title": product.displayName,
                    "price": product.displayPrice,
                    "period": periodLabel(product.subscription?.subscriptionPeriod),
                ])
            case "purchase":
                let purchaseProduct = if let product {
                    product
                } else {
                    try await Product.products(for: [Self.productId]).first
                }
                guard let product = purchaseProduct else {
                    respond(requestId, ["status": "error", "error": "unavailable"])
                    return
                }
                self.product = product
                switch try await product.purchase() {
                case .success(let verification):
                    await respondVerified(requestId, verification)
                case .userCancelled:
                    respond(requestId, ["status": "error", "error": "cancelled"])
                case .pending:
                    respond(requestId, ["status": "error", "error": "pending"])
                @unknown default:
                    respond(requestId, ["status": "error", "error": "failed"])
                }
            case "restore":
                try await AppStore.sync()
                var active = false
                for await result in StoreKit.Transaction.currentEntitlements {
                    if case .verified(let transaction) = result, transaction.productID == Self.productId {
                        active = true
                        await transaction.finish()
                    }
                }
                respond(requestId, ["status": active ? "active" : "locked"])
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
            respond(requestId, ["status": "error", "error": "unavailable"])
        }
    }

    private func handle(transactionResult: VerificationResult<StoreKit.Transaction>) async {
        guard case .verified(let transaction) = transactionResult, transaction.productID == Self.productId else {
            return
        }
        await transaction.finish()
    }

    private func respondVerified(_ requestId: String, _ verification: VerificationResult<StoreKit.Transaction>) async {
        guard case .verified(let transaction) = verification else {
            respond(requestId, ["status": "error", "error": "unverified"])
            return
        }
        guard transaction.productID == Self.productId else {
            respond(requestId, ["status": "error", "error": "failed"])
            return
        }
        let expiration = transaction.expirationDate?.timeIntervalSince1970 ?? 0
        await transaction.finish()
        respond(requestId, ["status": "active", "expiresAtMillis": Int64(expiration * 1000)])
    }

    private func periodLabel(_ period: Product.SubscriptionPeriod?) -> String {
        guard let period else { return "month" }
        switch period.unit {
        case .day: return period.value == 1 ? "day" : "days"
        case .week: return period.value == 1 ? "week" : "weeks"
        case .month: return period.value == 1 ? "month" : "months"
        case .year: return period.value == 1 ? "year" : "years"
        @unknown default: return "period"
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
        let viewController = MainViewControllerKt.MainViewController()
        viewController.modalPresentationStyle = .fullScreen
        viewController.additionalSafeAreaInsets = .zero
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
