# Impostor

Mobilna gra towarzyska typu pass-and-play z ukrytymi rolami. Jeden telefon krąży między graczami: agenci poznają tajne hasło, a impostor próbuje je odgadnąć, nie zdradzając swojej roli.

Strona wsparcia i polityka prywatności: [jordanrafalm.github.io/impostor](https://jordanrafalm.github.io/impostor/).

## Rozgrywka

1. Dodaj co najmniej trzech graczy.
2. Wybierz kategorię oraz liczbę impostorów. Obsługa jednoczesnego wyboru wielu kategorii jest wymaganiem do wdrożenia opisanym w ADR-0006, ale obecny ekran wybiera jedną kategorię.
3. Każda osoba przytrzymuje ekran, aby odsłonić swoją rolę, a następnie oddaje telefon kolejnej osobie.
4. Agenci widzą wspólne hasło. Impostor otrzymuje tylko oznaczenie roli lub opcjonalną wskazówkę.
5. Po ujawnieniu wszystkich ról aplikacja wskazuje osobę rozpoczynającą dyskusję.
6. Gracze zadają pytania, próbują odnaleźć impostora, a impostor stara się rozpoznać hasło.

Gra działa lokalnie i nie wymaga konta ani połączenia z siecią do podstawowej rozgrywki. Zawiera katalog haseł, pięć darmowych rozgrywek oraz opcjonalną subskrypcję odblokowującą pełny katalog.

## Najważniejsze funkcje

- Lokalna rozgrywka dla 3 lub większej liczby osób.
- Losowanie hasła, ról i osoby rozpoczynającej.
- Wybór kategorii oraz liczby impostorów.
- Efekt lodowego odsłaniania roli wymagający przytrzymania ekranu.
- Lokalne zapamiętywanie nazw graczy i ostatnio wybranych kategorii.
- Przypomnienie o grze oraz opcjonalny dźwięk i haptyka.
- Android i iOS z jednym współdzielonym rdzeniem w Kotlin Multiplatform.

## Architektura

Projekt to greenfieldowa aplikacja Kotlin Multiplatform (KMP) z w pełni współdzielonym UI w Compose Multiplatform. Kod podzielony jest na trzy moduły Gradle o jednokierunkowej zależności: `composeApp -> shared:domain` oraz `shared:data -> shared:domain`. Moduł domenowy nie zależy od żadnej infrastruktury (brak Firebase, brak Compose, brak API platformowych) — to czysty Kotlin, w pełni testowalny bez emulatora czy symulatora.

```mermaid
flowchart LR
    UI[composeApp<br/>Compose Multiplatform UI] --> Domain[shared:domain<br/>czyste reguły gry]
    Data[shared:data<br/>repozytoria] --> Domain
    UI --> Platform[Adaptery Android / iOS<br/>preferencje, haptyka, powiadomienia]
    Data --> Seed[Katalog haseł<br/>prompts-pl.json / prompts-en.json, offline]
    Data -. opcjonalnie .-> Firebase[Firebase<br/>Firestore / Analytics]
    Data -. opcjonalnie .-> Store[Google Play Billing /<br/>StoreKit 2]
```

### Moduły

**`shared:domain`** — reguły gry niezależne od platformy:
- `GameDomain.kt` — model `Prompt`/`Category`/`Role`, losowanie przydziału ról i hasła, logika `revealText()` ujawniająca hasło agentom i podpowiedź (lub samo oznaczenie roli) impostorowi.
- `CryoReducer.kt` — maszyna stanów efektu „lodowego” odsłaniania roli (przytrzymanie ekranu).
- `Trial.kt` / `Subscription.kt` — reguły darmowych rozgrywek próbnych i uprawnień premium.
- `RemoteCode.kt`, `PlayerNamesCodec.kt` — walidacja kodów promocyjnych i kodowanie lokalnej listy graczy.

**`shared:data`** — implementacje repozytoriów za interfejsami z domeny, z adapterami `expect`/`actual` dla Android, iOS i JVM (testy):
- `SeedCatalogRepository` — wczytuje i paruje osadzony katalog haseł zależny od języka (`prompts-pl.json` / `prompts-en.json`), buduje mapę podpowiedzi.
- `BundledCatalogRepository.*.kt` — platformowe ładowanie zasobu JSON (Android assets, iOS bundle, JVM classpath).
- `TrialRepository`, `SubscriptionRepository`, `SubscriptionPlatform.*.kt` — lokalny licznik prób oraz adaptery zakupów (Google Play Billing na Androidzie, StoreKit 2 przez most Swift na iOS).
- `FirebaseCodeServices.kt`, `RemoteCodeServices.kt`, `FirebasePlatformServices.*.kt` — opcjonalna warstwa zdalnych kodów promocyjnych i telemetrii Firebase; aplikacja działa w pełni offline bez tych plików konfiguracyjnych.

**`composeApp`** — współdzielone ekrany Compose (`App.kt`), nawigacja oparta o stan (bez biblioteki nawigacyjnej), efekt wizualny `IceBackdrop.kt` oraz adaptery platformowe (`*.android.kt` / `*.ios.kt`) dla preferencji graczy/kategorii, haptyki, dźwięku i lokalnych powiadomień (`PartyReminderReceiver` na Androidzie).

### Przepływ danych w rozgrywce

1. `App.kt` pobiera wybrane kategorie i liczbę graczy/impostorów z `CategoryPreferences` / `PlayerPreferences`.
2. `shared:domain` losuje hasło z katalogu (`SeedCatalogRepository` z `shared:data`), przydziela role i kolejność ujawniania.
3. Każdy gracz przytrzymuje ekran (`CryoReducer`) — `Prompt.revealText(role, hintsEnabled)` pokazuje agentom hasło, a impostorowi podpowiedź (jeśli włączona) lub samo oznaczenie roli, nigdy alias/identyczny tekst hasła.
4. Po odsłonięciu wszystkich ról aplikacja wskazuje osobę rozpoczynającą dyskusję.

### Dane gry

The app follows the first supported system language (`pl`, `en` or `uk`); unsupported-only preferences use English. The bundled catalogs are `shared/data/src/commonMain/resources/seed/prompts-pl.json` and `prompts-en.json`, with stable category and prompt IDs across languages. The English catalog includes translated prompt text and curated hints; the loader safely falls back to Polish if it is missing or invalid. The data module contains a locale-aware Firestore snapshot repository, but the current `ImpostorApp` is wired to `bundledCatalogRepository()` and does not yet load the cached remote snapshot or launch a background refresh. Treat cached-first refresh and same-ID remote-content replacement as pending integration requirements in [ADR-0006](docs/adr/ADR-0006-firebase-prompt-catalog-sync.md), not current runtime behavior. Remote prompt documents without `locale` are intended to remain Polish for compatibility. See [docs/firebase.md](docs/firebase.md).

### Firebase i płatności (opcjonalne)

Firebase jest przeznaczony wyłącznie do opcjonalnej dystrybucji treści i telemetrii — nigdy do rozgrywki offline ani logiki uprawnień. Repozytorium zdalnego katalogu istnieje, ale nie jest podłączone do uruchamiania aplikacji; zob. [ADR-0006](docs/adr/ADR-0006-firebase-prompt-catalog-sync.md). Jedynym źródłem prawdy dla subskrypcji premium jest zweryfikowana transakcja sklepu (Google Play Billing / StoreKit 2); lokalny cache sam nie odblokowuje kategorii. Reconciliation przy starcie/wznowieniu i aktualizacje StoreKit pozostają do wdrożenia zgodnie z [ADR-0003](docs/adr/ADR-0003-store-subscription-entitlements.md).

Więcej szczegółów architektonicznych, w tym decyzje projektowe, znajduje się w [docs/adr](docs/adr) oraz [docs/architecture.md](docs/architecture.md).

## Wymagania

- JDK zgodne z Android Gradle Plugin 8.10.
- Android SDK z API 36 dla Androida.
- Xcode 16+ dla iOS.
- XcodeGen dla wygenerowania hosta iOS z `iosApp/project.yml`.

## Szybki start: Android

```bash
./gradlew :shared:domain:jvmTest :shared:data:jvmTest
./gradlew :composeApp:assembleDebug
```

Kompilacja Androida i lokalna rozgrywka mają działać bez `google-services.json`. Opcjonalność Firebase na iOS nie została jeszcze zweryfikowana: `iosApp/iosApp.swift` wywołuje obecnie `FirebaseApp.configure()` bezwarunkowo. Wymóg uruchamiania bez konfiguracji i luka w weryfikacji są opisane w [ADR-0002](docs/adr/ADR-0002-optional-firebase-integration.md).

## Szybki start: iOS

```bash
./gradlew :composeApp:assembleComposeAppReleaseXCFramework
cd iosApp
xcodegen generate
open Impostor.xcodeproj
```

Wybierz symulator lub urządzenie w Xcode i uruchom schemat `Impostor`. Szczegóły są w [iosApp/README.md](iosApp/README.md).

## Firebase i płatności

Podstawowa rozgrywka oraz katalog haseł działają offline. Firebase jest opcjonalny i służy do funkcji zdalnych oraz telemetrii. Produkcyjne pliki `google-services.json` i `GoogleService-Info.plist` nie są częścią repozytorium. Instrukcja lokalnej konfiguracji znajduje się w [docs/firebase-setup.md](docs/firebase-setup.md).

Subskrypcja używa identyfikatora produktu `com.impostor.app.premium.monthly`. Aby przetestować zakupy, skonfiguruj odpowiedni produkt w Google Play Console i App Store Connect dla własnych identyfikatorów aplikacji.

## Testy

```bash
./gradlew :shared:domain:jvmTest :shared:data:jvmTest
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Testy domenowe obejmują walidację ustawień, przydzielanie ról, katalog haseł, trial, subskrypcje i reduktor lodowego odsłaniania.

## Prywatność

Gra nie wymaga konta. Nazwy graczy i ustawienia są przechowywane lokalnie na urządzeniu. Nie dodawaj do repozytorium plików Firebase, kluczy, identyfikatorów transakcji ani danych ze środowiska produkcyjnego.

## Licencja

Nie dodano jeszcze licencji. Przed publicznym udostępnieniem wybierz licencję odpowiadającą sposobowi wykorzystania projektu.
