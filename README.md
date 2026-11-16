# Kaash

A personal expense tracker for Android. It reads your bank's transaction SMS alerts, tags each transaction by payee, and tracks spending against budgets and recurring bills. Everything stays on the phone: no server, no account, no internet needed.

Supported banks today: **Federal Bank** and **Canara Bank** (UPI, card, and credit alerts). Other banks need a parser (see [Adding a bank](#adding-a-bank)).

## Features

- **Automatic import:** new bank SMS are read as they arrive, and your existing inbox is scanned once on first launch. Duplicates are skipped.
- **Tagging:** tag a transaction once and every past and future transaction from that payee gets the same category.
- **Transactions:** a month view with search and filters (debit/credit, untagged, account, category).
- **Summary:** spending per category for the month, plus total spent vs received.
- **Budgets:** a monthly limit per category, with a notification at 80% and 100%.
- **Bills:** recurring bills (weekly/monthly/yearly) with reminders. A bill is marked paid automatically when a matching debit arrives.
- **Calendar:** a month grid showing daily spend and bill due dates.
- **Unrecognised SMS:** bank messages that couldn't be parsed are listed so you can add a pattern for them.

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | Platform 35, Build-Tools 35 |
| Android phone | Android 8.0 (API 26) or newer, with a SIM that receives the bank SMS |

Gradle itself is not needed; the project ships with the Gradle wrapper (`gradlew`).

## Local setup

### 1. Install the JDK and Android SDK

**Easiest:** install [Android Studio](https://developer.android.com/studio). It bundles a JDK and installs the SDK for you. Open the project folder and let it sync, then skip to step 3.

**Command line only (Windows example):**

```powershell
# JDK 17
winget install Microsoft.OpenJDK.17

# Android command-line tools: download "Command line tools only" from
# https://developer.android.com/studio#command-line-tools-only
# and unzip so that this path exists:
#   %LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat

# Environment variables (open a new terminal afterwards)
setx JAVA_HOME "C:\Program Files\Microsoft\jdk-17.<version>-hotspot"
setx ANDROID_HOME "%LOCALAPPDATA%\Android\Sdk"

# SDK packages
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

On macOS/Linux it's the same idea: install JDK 17 (e.g. `brew install openjdk@17`), unzip the command-line tools to `~/Android/Sdk/cmdline-tools/latest`, and export `JAVA_HOME` and `ANDROID_HOME`.

### 2. Point the project at your SDK

Create `local.properties` in the project root (it is git-ignored):

```properties
# Windows (escape backslashes)
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
# macOS/Linux
# sdk.dir=/Users/<you>/Library/Android/sdk
```

This file is optional if `ANDROID_HOME` is set.

### 3. Build and test

```bash
./gradlew testDebugUnitTest   # unit tests (parsers, budgets, bill schedule)
./gradlew assembleDebug       # builds app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat` instead of `./gradlew`.

> If the first run times out downloading Gradle, retry, or download `gradle-8.11.1-bin.zip` manually and place it in `~/.gradle/wrapper/dists/gradle-8.11.1-bin/<hash>/` alongside an empty `gradle-8.11.1-bin.zip.ok` file.

### 4. Install on your phone

1. On the phone: **Settings → About phone →** tap **Build number** 7 times, then **Developer options → USB debugging** on.
2. Connect by USB, accept the prompt on the phone, and check it's listed:
   ```bash
   adb devices
   ```
3. Install:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Open **Kaash** and tap **Grant access** to allow SMS and notifications.

**Android 13+ note:** apps installed outside the Play Store can't be given SMS access until you unlock it. If the permission is greyed out or blocked, go to **Settings → Apps → Kaash → ⋮ (top right) → Allow restricted settings**, then grant again.

### Trying it without real SMS

Debug builds show a bug icon on the **Txns** tab. It injects six sample bank SMS so you can try tagging, budgets and the calendar without spending money.

## How it works

```
SMS_RECEIVED receiver ─┐
Inbox scan ────────────┴─► BankParsers ─► TxnRepository.ingest()
                                            ├─ dedupe (UPI ref / RRN, or account+amount+time)
                                            ├─ apply merchant rule → category
                                            ├─ save (Room)
                                            ├─ budget check → notification
                                            └─ mark matching bill paid
DailyBillWorker (WorkManager, daily) ─► due/overdue bill notifications
Compose UI ◄─ Room Flows
```

Stack: Kotlin, Jetpack Compose (Material 3), Room, WorkManager. No other dependencies, and no network access.

### Project layout

```
app/src/main/java/dev/expensetracker/
├── parse/BankParsers.kt     SMS regexes per bank, sender allowlist
├── parse/Samples.kt         Sample SMS (tests + debug inject)
├── data/                    Room entities, DAO, database, TxnRepository
├── ingest/                  SMS receiver + inbox backfill
├── work/DailyBillWorker.kt  Bill reminders
├── ui/                      One file per tab
├── App.kt                   Database/repository setup, schedules the worker
├── MainActivity.kt          Bottom navigation
├── Money.kt                 ₹ formatting, month ranges
└── Notifier.kt              Local notifications
app/src/test/                Unit tests
```

## Adding a bank

1. Find your bank's SMS sender ID (e.g. `AX-HDFCBK`) and add the stable part (`HDFCBK`) to `BANK_SENDERS` in `parse/BankParsers.kt`. SMS from senders not on this list are ignored. This also stops ordinary phone numbers from faking a bank alert.
2. Add a `Rule` with a regex using the named groups `amt`, `acct`, and optionally `party`, `ref`, `bal`.
3. Add the SMS text to `parse/Samples.kt` and a test case in `BankParsersTest.kt`, then run `./gradlew testDebugUnitTest`.
4. Messages from a listed bank that mention an amount but don't match any rule appear under the **Unrecognised SMS** icon on the Txns tab. Copy the text from there to write new rules.

## Data and privacy

- All data lives in a local SQLite database (`expenses.db`) on the phone.
- Android Auto Backup (if enabled on the phone) copies it to your Google Drive. There is no in-app export yet.
- The app has no internet permission and sends nothing anywhere.

## Known limitations

- Only Federal Bank and Canara Bank SMS formats are recognised.
- Reading banking/UPI app notifications is not implemented yet.
- Transaction time is when the SMS arrived, not the date written in the message.
- Categories are a fixed list of nine.
- Sideload only: Play Store policy doesn't allow SMS-reading apps of this kind.
