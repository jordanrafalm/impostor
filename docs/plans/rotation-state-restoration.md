# Plan: zachowanie stanu aplikacji po obrocie ekranu

## Cel

Po zmianie orientacji Android może standardowo odtworzyć `MainActivity`, ale aplikacja ma wrócić do bieżącego ekranu i zachować konfigurację oraz przebieg aktywnej gry zamiast zaczynać od Home.

## Decyzja

Użyć saveable state Compose w `commonMain`, bez deklarowania `android:configChanges` i bez blokowania orientacji. Zapisać trasę wraz z danymi sesji, listę graczy, wybrane kategorie, liczbę impostorów, ustawienie podpowiedzi i błąd walidacji. Ekran wejściowy nie może nadpisywać odtworzonej trasy podczas startowego `LaunchedEffect`.

Stan wymagający ochrony treści (animacja rozmrażania, potwierdzenie bramki tożsamości, aktywny gest) nie będzie przywracany: po obrocie ujawnianie zacznie się ponownie od zamrożonej karty i bramki „TO JA”. Zachowujemy natomiast ten sam indeks gracza i hasło w aktywnej sesji.

## Zakres

- Dodać saver dla `Route` i `GameSession`, serializujący sesję wyłącznie do typów obsługiwanych przez `rememberSaveable`.
- Zmienić stan główny w `ImpostorApp` na `rememberSaveable`; dodać saveery dla `List<Player>`, `Set<String>` i nullable `SetupValidation.Reason`.
- Zmienić startowy efekt tak, aby przejść ze Splash do Home tylko przy świeżym stanie początkowym.
- Zapobiec ponownemu wywołaniu inicjalizacji przypomnienia/żądania uprawnień przy samym odtworzeniu Activity.
- Zachować wpisywanie nazwy gracza i stan tymczasowo usuniętych domyślnych graczy podczas odtworzenia `PlayersScreen`.
- Resetować transient reveal state celowo, aby rotacja nie pozostawiała ujawnionego hasła.
- Nie zmieniać manifestu, domeny, trwałego przechowywania ani logiki rozgrywki.

## Weryfikacja

- Testy jednostkowe wspólnego modułu UI/app, jeśli istnieją; w przeciwnym razie kompilacja Androida i dostępne testy Gradle.
- Sprawdzić, że saver sesji round-trip zachowuje prompt, role, startera, podpowiedzi i `revealIndex`.
- Manualnie obrócić urządzenie na Home, Players, Categories, Reveal i GameStart; na Reveal potwierdzić, że gracz jest ten sam, a hasło znów zasłonięte.
