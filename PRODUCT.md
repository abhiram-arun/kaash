# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

One person: the owner, using it on their own Pixel phone. Kaash is a personal tool and will stay that way: sideloaded, never published, no other users or accounts.

## Product Purpose

Kaash shows where the owner's money went. It reads the bank's transaction SMS automatically, so every UPI, card and credit transaction lands in the app with no manual entry. The owner tags each one with a topic and subtopic. Success means opening the app and understanding the month's spending by category and by bank account at a glance, without having typed a single transaction.

Secondary jobs, in priority order: staying under monthly budgets, not missing recurring bills, and checking current account balances.

## Positioning

It reads data the owner's own bank already sends, on the device, with no server, login or third-party aggregator. Tag a payee once and every past and future transaction from that payee is tagged too.

## Operating Context

- Data arrives from bank SMS alerts as they come in, plus a one-time scan of the existing inbox. Each SMS carries amount, account last digits, payee, a UPI/RRN reference and usually the balance after the transaction.
- Banks in scope: Federal Bank (savings a/c ··8868, credit cards ··6612 and ··5599) and Canara Bank (a/c ··437).
- Typical sessions are short phone glances: reviewing new untagged transactions, checking this month's spend, looking at a bill or balance.
- Notifications are the main way back into the app: budget thresholds, bill reminders, and new untagged transactions.

## Capabilities and Constraints

- Native Android: Kotlin, Jetpack Compose (Material 3), Room, WorkManager. Single `:app` module, minSdk 26.
- Everything stays on the device; the app has no internet permission. Backup relies on Android Auto Backup.
- Sideload only, because Play Store policy doesn't allow this kind of SMS reading. On Android 13+ the owner must "Allow restricted settings" before granting SMS access.
- Categories are two levels: 18 topics and 132 subtopics (data/Taxonomy.kt). Budgets are set per topic; subtopic spend counts toward its topic.
- Money is stored as integer paise and shown in INR.
- The transaction time is when the SMS arrived, not the date written in the text.
- Undecided: reading banking/UPI app notifications (waiting on sample texts), statement import, custom categories, and data export.

## Brand Commitments

- The name is **Kaash**.
- Kaash is dark-only and always uses the dark theme, whatever the phone setting.
- The owner's palette reference, `D:/dev/1_3U5Md2C2Ni1s-ckj1LgM_w.jpg`, is binding: dark slate and navy surfaces, a mint-green primary, and a teal/cyan secondary.

## Evidence on Hand

- Six real SMS formats (Federal and Canara) in `app/src/main/java/dev/expensetracker/parse/Samples.kt`, covered by unit tests.
- The owner's real transaction history on the device. Never copy it into the repository, screenshots or docs beyond the samples above.
- Known unparsed formats on the device: Federal "UPI mandate" debits, and declined-card and OTP messages.

## Product Principles

1. **Zero entry.** If the bank sent it, Kaash already has it. Manual input is limited to tagging and to things banks don't send, such as bills and budgets.
2. **Understanding over bookkeeping.** Every screen should answer "where did my money go?" faster than the bank app can.
3. **Tag once, reuse forever.** Each manual tag should save future effort by teaching the app about that payee.
4. **Private by construction.** No network, no account, no third party, and nothing leaves the phone except Android's own backup.
5. **Trust the numbers.** Amounts, balances and dedupe must be exact. A missed or doubled transaction is worse than a plain screen.
