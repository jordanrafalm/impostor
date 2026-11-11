# ADR-0008: Lokalna promocja Black Friday

## Status

Accepted

## Kontekst

Lokalny limit próbny zapisuje liczbę gier i identyfikator ostatniej sesji. Promocja dodaje jednorazowo pięć gier, bez zmiany wykorzystanego limitu ani ograniczania Premium i zawsze darmowych kategorii.

## Decyzja

Repozytorium limitu przechowuje przyznanie promocji, zużycie obu pul i identyfikatory rozliczonych sesji w jednym wersjonowanym zapisie JSON w tym samym lokalnym magazynie (SharedPreferences / NSUserDefaults). Pierwszy odczyt migruje dotychczasowe wartości przez fallback do istniejących kluczy; stare klucze nadal są aktualizowane. Istniejący ostatni identyfikator jest zachowywany, lecz wcześniejsze identyfikatory nie były zapisywane i nie mogą zostać odtworzone. Nowe identyfikatory rozliczonych sesji są zachowane trwale.

Przy rozpoczęciu ograniczonej gry najpierw używana jest pula próbna, potem promocyjna. Ponowne przyznanie nie zmienia zużycia. Odrzucenie oferty pozostaje stanem bieżącego uruchomienia, także przy odtworzeniu interfejsu; przyznanie jest trwałe. Promocja nie ma określonej daty końca. Oferta poprzedza istniejący dialog kategorii i systemową prośbę o powiadomienia.

## Konsekwencje

- Stan jest lokalny dla instalacji, bez synchronizacji między urządzeniami; odinstalowanie lub wyczyszczenie danych go usuwa.
- Identyfikatory są przechowywane tylko dla rozliczonych gier, a więc ich liczba jest ograniczona liczbą dostępnych gier.
- Starsze wersje aplikacji nie znają nowego stanu; nie zapewniamy zgodności licznika przy powrocie do starszej wersji.
- Odkrywanie nadal używa dotychczasowych gestów, a napis tożsamości jest jednorazowym stanem UI dla bieżącego gracza.
