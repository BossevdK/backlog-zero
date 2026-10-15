# TODO / ideeën

Bijgewerkt op 2026-10-15.

## Nog te bouwen

1. **Series: seizoen en aflevering bijhouden.** Bij het type Serie kun je aangeven bij welk seizoen en welke aflevering je bent. Vraagt nieuwe velden op `MediaItem` en de item-tabel, dus een Room-migratie (schema's staan in `app/schemas/`, versie in `AppDatabase` verhogen, voorbeeld: `MIGRATION_1_2`) en aanpassingen in `EditScreen`/`DetailScreen`.
2. **Meer bronnen voor games en series.** Kijken naar andere plekken om meer games en series vandaan te halen, omdat TMDB en RAWG niet alles vinden. Nog uit te zoeken; denk aan bijvoorbeeld IGDB, TheTVDB of Trakt. Past achter `MetadataSearch` in `data/MetadataSearch.kt`; resultaten van meerdere bronnen samenvoegen en dubbelen weghalen.

## Klaar, nog te controleren op een toestel

- **Pick for me per categorie** (Alles/Games/Films/Series/Boeken) op het Pick-scherm en als snelknoppen op het dashboard.
- **Opgeruimde bibliotheek en itemscherm**: 3 kolommen, filterknop in de zoekbalk, poster-kop met statusbalk en kaarten.
- **Dashboard**: categoriefilter bovenaan; rijen Bezig, Volgende uit je backlog (prioriteit, dan wachttijd), Recent toegevoegd en Recent afgerond (`domain/HomeRows.kt`).
- **Steam-bibliotheek** (profiel > Steam-bibliotheek): haalt je games op met je eigen Steam-ID/profielnaam en je eigen gratis API-sleutel (steamcommunity.com/dev/apikey; 'Gamedetails' moet openbaar staan). Bij het zoeken naar games staan games die je al hebt bovenaan met "In je Steam-bibliotheek". Ze komen niet vanzelf in je backlog. Opgeslagen in Room (versie 2, `steam_games`); sleutel en ID alleen in de privé-opslag van de app. Code: `data/SteamLibrary.kt`, `data/OwnedGames.kt`, `ui/profile/SteamSection.kt`.
- **Zoekbalk en categorie blijven vast bovenaan** bij het toevoegen; resultaten en formulier scrollen eronder (`ui/edit/EditScreen.kt`).
- **Bronvermelding** (TMDB-verplichte tekst, RAWG, Open Library) onderaan het profielscherm.
- Migratie 1 naar 2 is vergeleken met het door Room gegenereerde schema, maar nog niet op een toestel met bestaande gegevens geprobeerd.

## Bevestigd door de gebruiker

- De `LocalLifecycleOwner`-crash is weg.
- Online zoeken werkt en de resultaten staan boven het toetsenbord.

## Werken op een andere laptop

- `git pull` haalt dit bestand en alle code binnen. Repo: `BossevdK/backlog-zero`.
- Bouwen: JDK 17, Android SDK; zie README. `local.properties` staat niet in git en bevat het SDK-pad van die laptop.
- Lokaal met online zoeken bouwen: zet `TMDB_API_KEY=...` en `RAWG_API_KEY=...` in `~/.gradle/gradle.properties` (niet in het project).
- Releases hoef je niet lokaal te bouwen: elke push naar `main` (behalve alleen `.md`-wijzigingen) maakt via GitHub automatisch een release met versie `1.<runnummer>`. De ondertekeningssleutel en API-sleutels staan als secrets in de repo.
- De update-knop in de app haalt de nieuwste release op; de geïnstalleerde app moet met dezelfde sleutel zijn ondertekend.
- Handmatig een release starten: `gh workflow run Release`.
