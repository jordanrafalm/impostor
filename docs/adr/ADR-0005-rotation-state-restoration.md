# ADR-0005: Odtwarzanie stanu aplikacji po zmianie orientacji

## Status

Accepted

## Kontekst

Android odtwarza `MainActivity` przy zmianie konfiguracji, w tym orientacji ekranu. Główny routing i konfiguracja gry są obecnie przechowywane w `remember` w `ImpostorApp`, więc po odtworzeniu kompozycji użytkownik wraca do ekranu początkowego, a aktywna runda może zostać utracona.

Impostor jest grą pass-and-play z ukrytymi rolami. Rotacja nie powinna zmieniać gracza ani losować nowej sesji, ale nie może też przypadkowo odsłonić hasła po odtworzeniu ekranu.

## Decyzja

Stan nawigacji i konfiguracji potrzebny do wznowienia bieżącego ekranu będzie przechowywany przez Compose `rememberSaveable`. Trasa gry zapisze wystarczające dane `GameSession` do odtworzenia tego samego promptu, przydziałów ról, startera, ustawienia podpowiedzi i indeksu ujawnianego gracza.

Nie wyłączamy standardowego odtwarzania `Activity` przez `android:configChanges`. Pozwala to Androidowi normalnie obsłużyć zmianę rozmiaru i konfiguracji, a Compose może ponownie ułożyć interfejs dla nowej orientacji. Zmiana stanu pozostaje w `commonMain` i może być współdzielona z hostem iOS.

Stan odkrywania hasła nie jest zapisywany. Po odtworzeniu `RevealScreen` karta pozostaje zasłonięta, a użytkownik ponownie potwierdza tożsamość przed przytrzymaniem. Sesja i indeks gracza pozostają zachowane.

## Konsekwencje

- Rotacja przywraca ekran, konfigurację gry i aktywną sesję zamiast wracać do Home.
- Efekt startowy nie ponawia inicjalizacji przypomnienia i żądania uprawnień tylko dlatego, że Activity zostało odtworzone.
- Saveery muszą kodować modele domenowe do prymitywnych wartości akceptowanych przez mechanizm `rememberSaveable`.
- Krótkotrwałe elementy UI oraz trwające animacje/interakcje mogą się zresetować; reveal celowo wraca do stanu bezpiecznego.
- Zachowanie ogranicza się do odtworzenia obsługiwanego przez system stanu Compose; nie wprowadza trwałego zapisu aktywnej rundy na dysk po ubiciu procesu.
