# Plan odświeżenia UI: ekran główny

## Cel

Przebudować ekran startowy Impostor zgodnie z dostarczonym makietowym kierunkiem: zimne, granatowe tło, wyraźny nagłówek, trzy ustawienia gry i dominujący przycisk rozpoczęcia. Każdy interaktywny przycisk ma reagować na najazd kursorem oraz jego opuszczenie przejazdem poświaty od lewej do prawej; na urządzeniach dotykowych ten sam przejazd uruchamia dotknięcie.

## Ustalenia

- Źródłem inspiracji jest `Mobile Party Game App`, wyłącznie jako referencja wizualna. Nie kopiujemy kodu React/Tailwind.
- Istniejący `IceBackdrop` zostaje wspólnym, animowanym tłem: gradient, aurora, krople i ziarno.
- Nie zmieniamy domeny, nawigacji, płatności ani Firebase. Zmiana pozostaje w `composeApp/commonMain`.
- W aplikacji mobilnej nie ma klasycznego hovera; efekt jest zapewniony dla myszy/trackpada, a jego dotykowym odpowiednikiem jest tap.

## Kroki wdrożenia

1. Zdefiniować centralne tokeny wizualne lodowego UI (tło, szkło, obramowanie, tekst i akcent) w warstwie Compose.
2. Dodać reużywalny `FrostButton`: dostępny target 48 dp, stan wciśnięcia, stan hover oraz animowany pasek światła przesuwający się z lewej do prawej przy wejściu i wyjściu kursora.
3. Przebudować Home: logotyp, trzy panele ustawień (gracze, kategorie, impostorzy), CTA „GRAJ” i wejście do Premium.
4. Zastosować `FrostButton` do istniejących akcji na pozostałych ekranach, aby zachować jedną interakcję w całej aplikacji.
5. Zbudować Android oraz framework iOS; ręcznie sprawdzić na urządzeniu/emulatorze kontrast, hit targety i przejścia.

## Kryteria akceptacji

- Home prezentuje strukturę i kontrast z makiety, bez zmiany zachowania konfiguracji gry.
- Przycisk otrzymuje przejazd poświaty zarówno przy hover enter, jak i hover exit; tap uruchamia identyczny efekt.
- UI i animacje działają z jednego kodu `commonMain` dla Androida oraz iOS.
- Brak nowych zależności i brak dostępu Firebase w UI.
