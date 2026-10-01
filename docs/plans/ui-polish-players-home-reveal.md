# Plan: poprawki UI (gracze, home, rozmrażanie)

## Kontekst

Trzy drobne poprawki UX, bez zmiany architektury czy granic modułów. Wszystko mieści się w `composeApp/src/commonMain` poza punktem 3, który wymaga też implementacji iOS.

## 1. Ekran „Gracze” — pole dodawania na dole

`PlayersScreen` (composeApp/src/commonMain/kotlin/com/impostor/app/App.kt, ok. L779-912) obecnie renderuje w kolejności: nagłówek → wiersz „Wpisz imię gracza… + [+]” → przewijalna lista graczy → `FrostButton("GOTOWE")`.

**Zmiana:** przenieść wiersz z `BasicTextField` + przyciskiem „+” tak, aby znajdował się bezpośrednio nad przyciskiem „GOTOWE”, pod listą graczy (kolejność: nagłówek → lista graczy w `Column(weight=1f)` → wiersz dodawania gracza → GOTOWE).

- Żadnej zmiany logiki (`normalizePlayerName`, `onPlayers`, limit graczy) — czysto layoutowa zmiana kolejności elementów w `Column`.
- Upewnić się, że `imePadding()` nadal działa poprawnie z klawiaturą — pole tekstowe blisko dolnej krawędzi/klawiatury jest bardziej naturalne (uzasadnienie UX tej zmiany).
- Zachować `Spacer(Modifier.height(...))` odstępy analogicznie do obecnych.

## 2. Ekran główny — okresowa animacja przycisków

`FrostButtonSurface` (App.kt ok. L563-637) ma już dwa elementy „animacji po naciśnięciu”:
- `pressScale` (skala 0.965 przy `pressed`),
- `frostSweep` uruchamiane przez `runSweep()` (pasek światła przesuwający się przez przycisk), wywoływane w `onClick`/hover-exit.

Na ekranie `CategoriesScreen` te same przyciski (`FrostButton`) używają identycznego mechanizmu — nie ma osobnej animacji „kategorii”. Efekt, o który prosi użytkownik, to **auto-wyzwalanie `runSweep()` (+ opcjonalnie `pressScale`) w regularnych odstępach czasu**, gdy przycisk jest bezczynny — czyli „przypomnienie” wizualne na Home.

**Zmiana:**
- Dodać do `FrostButtonSurface` opcjonalny parametr `autoPulse: Boolean = false`.
- Gdy `autoPulse == true`, uruchomić `LaunchedEffect(Unit)` z pętlą `while (isActive) { delay(4-6 s); runSweep() }` (użyć istniejącego `runSweep()`, żeby był to dokładnie ten sam efekt co przy tap/hover).
- Włączyć `autoPulse = true` na Home dla głównych CTA: `FrostButton("G R A J", style = Primary)` i `FrostButton("ODBLOKUJ PEŁNĄ WERSJĘ", style = Subtle)`. Nie włączać na innych ekranach (Categories, Players) — tam efekt ma zostać wyłącznie reakcją na interakcję.
- Odstęp czasowy: rekomenduję 5000 ms, zsynchronizowany do startu ekranu (brak losowego jittera potrzebnego na start).
- Brak wpływu na dostępność: animacja czysto wizualna, nie zmienia focusu ani nie generuje dźwięku/wibracji.

## 3. Rozmrażanie (Reveal) — wibracja tylko podczas dotyku

Stan obecny:
- `ThermalFeedback` (composeApp/src/commonMain/kotlin/com/impostor/app/PlatformFeedback.kt) ma `start()/pulse()/complete()`.
- Android (`PlatformFeedback.android.kt`) już wibruje realnie (`Vibrator`/`VibrationEffect`) w `start`, cyklicznie w `pulse` (wywoływane co 120 ms w pętli w `RevealScreen`, App.kt ok. L1155-1173), i w `complete`.
- iOS (`PlatformFeedback.ios.kt`) ma **puste (no-op)** implementacje `start/pulse/complete` — na iOS rozmrażanie nie wibruje wcale.
- Gating dotyku jest już poprawny: `pulseJob` startuje na `awaitFirstDown()` i jest `cancel()`-owany w bloku `finally` po `waitForUpOrCancellation()`/anulowaniu gestu — czyli wibracja z definicji trwa tylko podczas kontaktu z ekranem. To wymaganie użytkownika jest już spełnione po stronie Android; brakuje go na iOS.

**Zmiana:** zaimplementować `IosThermalFeedback` z realnym haptic feedbackiem:
- Użyć `UIImpactFeedbackGenerator` (styl `.medium` dla `start`/`pulse`, `.light` lub `.rigid` dla subtelnego pulsu co 120 ms, `.heavy` lub `UINotificationFeedbackGenerator` dla `complete`).
- Rozważyć `prepare()` na `start()` dla mniejszego opóźnienia pierwszego impulsu.
- Nie dodawać własnego mechanizmu start/stop w warstwie iOS — cykl życia (tylko podczas dotyku) jest już zarządzany przez `RevealScreen`, więc implementacja iOS ma być symetryczna z Androidem (reaguje na wywołania, nie zarządza czasem).

## Zakres i wyłączenia

- Brak zmian w domenie (`CryoReducer` pozostaje bez zmian — logika stanu melt/freeze jest poprawna).
- Brak zmian w nawigacji, Firebase, płatnościach.
- Brak nowych zależności (UIKit haptics są częścią systemu na iOS).

## Weryfikacja

- `./gradlew :composeApp:testDebugUnitTest` (lub odpowiedni target testów commonMain/androidMain).
- Build frameworka iOS (`./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` lub zgodnie z `docs/build.md`) — konieczny, bo dotyka `iosMain`.
- Manualna weryfikacja na urządzeniu: kolejność pól na ekranie Gracze, okresowy „glow sweep” na Home bez interakcji, wibracja na iOS wyłącznie podczas przytrzymania ekranu w Reveal.

## ADR

Nie wymagany — brak zmiany architektury, granic modułów, trwałości danych, nawigacji/stanu czy sposobu współdzielenia kodu; to lokalne poprawki UI/haptics w istniejących komponentach.
