# Plan: podskok przycisku i napis tożsamości na lodzie

## Zmiany UX w `RevealScreen`

1. Przy aktywacji przycisk „NASTĘPNY GRACZ”/„ROZPOCZNIJ RUNDĘ” wykonuje wyższy, ale delikatny podskok w górę. Animacja pozostaje lokalna dla ekranu Reveal i resetuje się dla kolejnego gracza.
2. Napis „GRACZ X TO JA” jest rysowany bezpośrednio na zamrożonej karcie, bez osobnego czarnego tła. Pierwsze przytrzymanie płynnie wygasza napis, a dopiero po jego zniknięciu rozpoczyna animację rozmrażania.
3. Jeśli użytkownik puści ekran przed końcem zanikania napisu, napis łagodnie wraca i karta nie rozmraża się. Po pełnym zniknięciu napis nie pojawia się ponownie podczas kolejnych prób na tym samym ekranie. Przy zmianie gracza wraca nowy tekst „GRACZ X TO JA”.

## Zakres

- Zmiany wyłącznie w `RevealScreen` w `composeApp/src/commonMain/kotlin/com/impostor/app/App.kt`.
- Nie zmieniać współdzielonego `FrostButtonSurface`, logiki domenowej, nawigacji ani stanu rundy.

## Weryfikacja

- Uruchomić `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest`.
- Sprawdzić wizualnie na urządzeniu: tekst na lodzie bez czarnego panelu, zanikanie przed rozmrażaniem, brak powrotu tekstu na tym samym ekranie i wyższy podskok przycisku.
