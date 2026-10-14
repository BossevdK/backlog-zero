# Backlog Zero

Een rustige, donkere Android-app om je persoonlijke backlog van **games, films, series en boeken** bij te houden. Voor individueel gebruik: geen sociale functies.

## Wat zit er in versie 1.0

- Lokaal account (registreren, inloggen, uitloggen, account verwijderen); wachtwoorden worden met PBKDF2 gehasht, data is per gebruiker afgeschermd
- Bibliotheek met zoeken op titel en filters op categorie, status, prioriteit en favorieten
- Online zoeken bij het toevoegen: kies een resultaat en titel, maker, jaar, platform en omslag worden ingevuld (boeken: Open Library, films/series: TMDB, games: RAWG)
- Items toevoegen, bewerken en verwijderen, met omslag (uit je galerij), notities, beoordeling (1-5 sterren), jaar, maker en platform
- Statussen: backlog, bezig, gepauzeerd, afgerond
- Dashboard, **Pick for me** (gewogen toeval op prioriteit) en statistieken
- Laad-, fout- en lege toestanden op elk scherm

Nog niet: synchronisatie tussen apparaten, AI-aanbevelingen, import en betaalde functies. De architectuur is hierop voorbereid (zie hieronder).

## Techniek

| Onderdeel | Keuze |
| --- | --- |
| Taal / UI | Kotlin, Jetpack Compose, Material 3 (alleen donker) |
| Opslag | Room (SQLite), schema's in `app/schemas/` in git |
| Navigatie | Navigation Compose |
| Afbeeldingen | Coil |
| Tests | JUnit + kotlin.test (unit-tests voor domeinlogica en beveiliging) |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |

Alleen Android; er is bewust niet gekozen voor een cross-platform framework.

### Opbouw

```
app/src/main/java/com/backlogzero/app/
  domain/   Pure Kotlin: modellen, filters, Recommender (Pick for me), statistieken
  data/     Room-database, AuthRepository, LibraryRepository, CoverStorage
  ui/       Compose-schermen per onderdeel (auth, home, library, detail, edit, pick, stats, profile)
```

Uitbreidbaarheid:

- **Categorieën**: `MediaType` is een enum die als tekst (`key`) wordt opgeslagen. Een nieuw type toevoegen is één regel plus een icoon; geen databasemigratie.
- **Sync / cloud-account**: de schermen praten alleen met `AuthRepository` en `LibraryRepository`. Een Supabase-/Firebase-implementatie kan die interfaces later vervangen of aanvullen (`AppContainer` in `BacklogZeroApp.kt`).
- **Aanbevelingen / AI**: `Recommender` is een interface; `WeightedRandomRecommender` is de eerste implementatie.
- **Omslagen ophalen / import**: komen via `CoverStorage.import(...)` en de repository binnen.
- **Premium**: nog niets ingebouwd; hang het aan `AppContainer` zodra het nodig is.

## Installatie en bouwen

Vereist: JDK 17, Android SDK (platform 34, build-tools 34.0.0) en Gradle 8.7 (of Android Studio).

1. Kloon de repository.
2. Maak `local.properties` in de hoofdmap met het pad naar je SDK:
   ```
   sdk.dir=C\:\\Users\\<jij>\\android-tools\\sdk
   ```
3. Bouw en test:
   ```
   gradle testDebugUnitTest      # unit-tests
   gradle assembleDebug          # debug-APK in app/build/outputs/apk/debug/
   gradle assembleRelease        # release-APK (zie ondertekening)
   ```

De debug-app heeft een eigen id (`com.backlogzero.app.debug`) en kan naast de release-app staan.

## Configuratie en geheimen

Versie 1 heeft **geen** API-sleutels of server nodig. Wat wel geheim is, staat niet in git (zie `.gitignore`):

- `local.properties` (SDK-pad)
- `keystore.properties` en `*.jks` (ondertekeningssleutel)
- `.env*`

Zodra een backend of API wordt toegevoegd: zet sleutels in `local.properties`/GitHub Secrets, nooit in de code.

## Databasewijzigingen

Het Room-schema wordt bij elke build geëxporteerd naar `app/schemas/` en hoort mee in git. Bij een wijziging:

1. Verhoog `version` in `AppDatabase`.
2. Schrijf een `Migration` (nooit destructief migreren; gebruikersdata moet behouden blijven).
3. Commit de nieuwe schema-JSON samen met de migratie.

## Ondertekenen en publiceren

Elke release moet met **dezelfde sleutel** zijn ondertekend, anders kan Android de update niet over de oude installatie heen zetten.

1. Maak (eenmalig) een sleutel: `keytool -genkeypair -v -keystore backlogzero-release.jks -alias backlogzero -keyalg RSA -keysize 2048 -validity 10000`
2. Maak `keystore.properties` (niet in git):
   ```
   storeFile=sleutel/backlogzero-release.jks
   storePassword=...
   keyAlias=backlogzero
   keyPassword=...
   ```
3. Verhoog `versionCode` en `versionName` in `app/build.gradle.kts`.
4. `gradle assembleRelease`, en geef de APK een naam: `BacklogZero.apk`.

### Updates uitbrengen via GitHub

- Werk op een branch (`feature/...`), open een pull request; CI draait tests en bouwt de debug-APK.
- Test een wijziging door de debug-APK uit de CI-run (artifact) of lokaal te installeren (`adb install -r app-debug.apk`).
- Release: elke push naar `main` bouwt automatisch een release (`.github/workflows/release.yml`) met versie `1.<runnummer>`, tag `v1.<runnummer>` en `BacklogZero.apk` als bijlage. `versionName` en `versionCode` komen uit CI; je hoeft ze niet meer handmatig te verhogen. Vereist de secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` en `KEY_PASSWORD`; zonder die secrets mislukt de workflow bewust, omdat een debug-ondertekende APK niet als update werkt.

### iOS

Deze versie is alleen voor Android. Voor iOS zou een aparte app (of een herbouw in een cross-platform framework) nodig zijn; daarvoor is een Mac of een cloud-buildservice vereist.

## Werkafspraken

- Kleine, duidelijke commits; grotere veranderingen op een eigen branch en via een pull request.
- Geen voorbeeldgegevens of nepfunctionaliteit in de productieversie.

## Online zoeken (API-sleutels)

Boeken zoeken zonder sleutel (Open Library). Films/series (TMDB) en games (RAWG) vragen een gratis sleutel; zonder sleutel staat zoeken voor dat type uit en vul je de velden zelf in.

1. Vraag een sleutel aan op themoviedb.org (Instellingen, API) en rawg.io/apidocs.
2. Lokaal: zet `TMDB_API_KEY=...` en `RAWG_API_KEY=...` in `~/.gradle/gradle.properties`.
3. Release via GitHub: voeg dezelfde namen toe als repository-secrets (`gh secret set TMDB_API_KEY`).

De sleutels worden in de app gebouwd en zijn dus uit de APK te halen; gebruik alleen gratis sleutels die je kunt intrekken.
