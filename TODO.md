# TODO / ideeën

Bijgewerkt op 2026-10-14. Wat hier staat is nog niet gebouwd.

## Gewenste toevoegingen

1. **Pick for me per categorie.** De willekeurige keuze (`Recommender` / `PickScreen`) moet ook kunnen voor alleen films, alleen series, alleen games of alleen boeken, naast "alles".
2. **Homepagina beter sorteren.** Nu nog niet vastgelegd hoe; bedenk eerst een indeling (bijvoorbeeld "Bezig", "Volgende uit je backlog", "Recent toegevoegd" en per categorie). Bestand: `ui/home/HomeScreen.kt`.
3. **Steam-games inladen.** Importeer je Steam-bibliotheek, zodat je bij het online zoeken van games kunt zien welke je al hebt. Nog uit te zoeken:
   - Hoort een Steam-game die je bezit automatisch in je backlog, of alleen als markering in de zoekresultaten? (Ik ga uit van: markering bij het zoeken, plus optioneel importeren.)
   - Steam Web API vraagt een eigen API-sleutel en je Steam-ID (64-bit); het profiel moet "game details" openbaar hebben.
   - Past naast `data/MetadataSearch.kt` (RAWG-zoeken); koppel op titel of Steam app-id.
4. **Series: seizoen en aflevering bijhouden.** Bij het type Serie kun je aangeven bij welk seizoen en welke aflevering je bent. Vraagt nieuwe velden op `MediaItem` en `UserEntity`/item-tabel, dus een Room-migratie (schema's staan in `app/schemas/`, versie in `AppDatabase` verhogen) en aanpassingen in `EditScreen`/`DetailScreen`.

## Nog open van eerder

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
