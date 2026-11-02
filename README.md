# Coinop

A personal budget + investment tracker for Android. Native Kotlin, Jetpack Compose, Supabase backend,
retro "Arcadia" / "Cyberpunk" look. Package: `com.coinop.budget`.

## What it does
- **Banking** – create bank accounts yourself (name + the money in it today), then log money in/out as it
  happens. Move money between your own accounts (never counted as spending). Balance = opening balance + entries.
- **Categories & rules** – dynamic categories; keyword rules suggest a category from the note. Picking a
  category by hand is permanent for that entry (`is_manual_category`), rules never overwrite it.
- **Budget** – Year -> Month -> Week -> Day. A lower level can only be set after its parent exists
  (suggested split pre-filled), with a warning if lower budgets add up to more than the parent.
- **Invest** – create demat / fund accounts (Upstox, Groww, ...), add holdings by hand, switch between accounts
  with one tap, see value and P&L. Live prices: mutual funds via mfapi.in (AMFI NAVs), stocks via Yahoo Finance's
  public chart endpoint (unofficial, may rate-limit or change). See `data/Services.kt` to swap sources.
- **Home** – widgets (balances, net worth, budget progress, top categories): show/hide/reorder.
- **Settings** – UI style (Arcadia / Cyberpunk), light/dark/system, 25 colour themes, fonts, text size,
  page backdrops, currency (live rates), timezone, categories, rules.

## Setup
1. Create a Supabase project. In the SQL editor run `supabase/schema.sql`.
   Authentication > Providers > Email: turn **Confirm email off** (personal use) so sign-up signs you in.
2. In `local.properties` (Android Studio creates it with `sdk.dir`) add:
   ```
   SUPABASE_URL=https://YOUR-PROJECT.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_YOUR-KEY
   ```
   Without these the app shows a setup screen instead of crashing.
3. Open this folder in Android Studio (JDK 17), let Gradle sync, run on an emulator or device (Android 8.0+).
4. Create an account on the sign-in screen. Default categories, rules and widgets are seeded on first sign-in.

## Notes
- Currency is one app-wide setting. Changing it asks: **relabel** (numbers unchanged) or **convert** at the live
  rate (server function `convert_all_amounts`). Market prices are quoted in INR and converted for display.
- Timezone decides "today", week and month boundaries. Weeks run Monday-Sunday; the year is the calendar year.
- Holdings' average cost is stored per unit in INR.
- Dependency versions are pinned in `gradle/libs.versions.toml`; bump them if Android Studio suggests it.

## Layout
```
app/src/main/java/com/coinop/budget/
  data/      Supabase models + repositories, price/FX services, on-device prefs
  domain/    money, time, rule engine, budget period maths, stats
  ui/        ViewModel, shared components
  ui/theme/  palettes (25 themes x light/dark), sticker surface, shapes, backdrops, icons, fonts
  ui/screens Home, Banking, Invest, Budget, Settings, auth shell
supabase/schema.sql
```

## Versions and releases (GitHub Actions)

One workflow, `.github/workflows/release.yml`, does everything. Every push to `main` builds a signed APK,
tags it `vX.Y.Z` and publishes it on the Releases page; the commit message becomes the release notes.
Changes to `*.md`, `docs/` or `.gitignore` do not trigger it.

- The first release is **1.0.0**. Each push after that raises the last number by one (1.0.1, 1.0.2, ...).
- For a new minor or major version, open Actions > Release > Run workflow and pick `minor` or `major`.
- The app shows its version in Settings > About. A build made on your own machine says `1.0.0-dev`.

**One-time setup on GitHub**
1. Push this folder as the root of the repository (so `.github/` and `gradlew` sit at the top).
2. In Settings > Secrets and variables > Actions, add:
   - `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`: the same two values as in `local.properties`.
     The publishable key is meant to be public (row level security protects the data), so it is fine inside the APK.
   - Recommended, so every release is signed with the same key and updates install over each other:
     `ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Create the keystore once and keep a backup of it:
     ```
     keytool -genkeypair -v -keystore release.jks -alias coinop -keyalg RSA -keysize 2048 -validity 10000
     base64 -i release.jks | pbcopy        # macOS; on Linux use: base64 -w0 release.jks
     ```
     Paste that text into `ANDROID_KEYSTORE_BASE64`, and use the passwords and alias you chose.
     Without them, releases are signed with a debug key and a newer version may need the old one uninstalled first.
3. In Settings > Actions > General, set Workflow permissions to "Read and write permissions".

A release that already exists is never overwritten. The two personal-use display fonts are not committed
(see `.gitignore`), so builds made on GitHub use Rajdhani in their place.
