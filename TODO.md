# TODO / ideeën

Bijgewerkt op 2026-10-14. Wat hier staat is nog niet gebouwd.

## Gewenste toevoegingen

1. ~~**Pick for me per categorie.**~~ Klaar (nog niet op een toestel gecontroleerd): keuze Alles/Games/Films/Series/Boeken op het Pick-scherm, en snelknoppen per categorie op de Pick-kaart van het dashboard (`ui/pick/PickScreen.kt`, `ui/home/HomeScreen.kt`). De bibliotheek heeft nu één rij categorieën plus een Filters-paneel voor status, prioriteit en favorieten (`ui/library/LibraryScreen.kt`).
2. **Homepagina beter sorteren.** Nu nog niet vastgelegd hoe; bedenk eerst een indeling (bijvoorbeeld "Bezig", "Volgende uit je backlog", "Recent toegevoegd" en per categorie). Bestand: `ui/home/HomeScreen.kt`.
3. **Steam-games inladen.** Importeer je Steam-bibliotheek. Games die je al hebt krijgen een markering in de online zoekresultaten en komen **bovenaan** de resultaten te staan (bevestigd door de gebruiker). Ze hoeven niet automatisch in je backlog.
   - Steam Web API vraagt een eigen API-sleutel en je Steam-ID (64-bit); het profiel moet "game details" openbaar hebben.
   - Past naast `data/MetadataSearch.kt` (RAWG-zoeken); koppel op titel (genormaliseerd) of Steam app-id. Sortering van de resultaten gebeurt na het zoeken.
4. **Series: seizoen en aflevering bijhouden.** Bij het type Serie kun je aangeven bij welk seizoen en welke aflevering je bent. Vraagt nieuwe velden op `MediaItem` en `UserEntity`/item-tabel, dus een Room-migratie (schema's staan in `app/schemas/`, versie in `AppDatabase` verhogen) en aanpassingen in `EditScreen`/`DetailScreen`.
5. **Zoekbalk blijft vast bij scrollen.** Bij online zoeken scrolt de zoekbalk nu mee met de rest van het formulier; hij moet bovenaan blijven staan terwijl je door de resultaten scrolt (bijvoorbeeld zoekveld buiten de scrollende kolom in `EditScreen.kt`, of een aparte zoekweergave).
6. **Meer bronnen voor games en series.** Kijken naar andere plekken om meer games en series vandaan te halen, omdat TMDB en RAWG niet alles vinden. Nog uit te zoeken; denk aan bijvoorbeeld IGDB, TheTVDB of Trakt. Past achter `MetadataSearch` in `data/MetadataSearch.kt`; resultaten van meerdere bronnen samenvoegen en dubbelen weghalen.

## Nog open van eerder

- Opgelost in code, nog niet op een toestel gecontroleerd: bij online zoeken zat het toetsenbord voor de resultaten. Ze worden nu in beeld gescrold (`SearchSection` in `EditScreen.kt`).
- De TMDB-vermelding ("This product uses the TMDB API but is not endorsed or certified by TMDB") staat nog niet in de app (bijvoorbeeld in het profielscherm). Dat is een voorwaarde van TMDB.
- Online zoeken voor films, series en games is op een toestel nog niet getest (release 1.3 bevat de sleutels).
- Of de `LocalLifecycleOwner`-crash weg is (opgelost met Compose BOM 2024.09.03) is op een toestel nog niet bevestigd.

## Werken op een andere laptop

- `git pull` haalt dit bestand en alle code binnen. Repo: `BossevdK/backlog-zero`.
- Bouwen: JDK 17, Android SDK; zie README. `local.properties` staat niet in git en bevat het SDK-pad van die laptop.
- Lokaal met online zoeken bouwen: zet `TMDB_API_KEY=...` en `RAWG_API_KEY=...` in `~/.gradle/gradle.properties` (niet in het project).
- Releases hoef je niet lokaal te bouwen: elke push naar `main` (behalve alleen `.md`-wijzigingen) maakt via GitHub automatisch een release met versie `1.<runnummer>`. De ondertekeningssleutel en API-sleutels staan als secrets in de repo.
- De update-knop in de app haalt de nieuwste release op; de geïnstalleerde app moet met dezelfde sleutel zijn ondertekend.
- Handmatig een release starten: `gh workflow run Release`.
