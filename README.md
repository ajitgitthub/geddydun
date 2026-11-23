# Geddydun

A reminder app for Android that's hard to ignore. Alarms ring until you answer them, read the title out loud, and can be shared between two people.

## Install

Open the [latest release](https://github.com/ajitgitthub/geddydun/releases/latest) on your phone and download **Geddydun.apk**. The app checks this page and offers updates by itself.

## What it does

- Full-screen alarms with "I'll do it now", snooze and "Not today"; an unanswered alarm rings again every 30 minutes
- Repeats: daily, weekly, monthly, yearly, every N days or weeks, weekdays, nth weekday, last day of the month
- "Count from when it's done" repeats, so a missed chore moves the next one along
- Bills with a payment history
- Shared shopping lists, and errands you can send to the other person
- Talking alarms: it says the title, and you can answer by voice
- Backup and restore with a code

## Build it yourself

You need Android Studio (or JDK 17 and Gradle 8.9) and your own Firebase project.

1. In the [Firebase console](https://console.firebase.google.com), create a project and add an Android app with the package name `com.personal.remindme`.
2. Turn on **Authentication → Anonymous** and **Firestore Database**.
3. Download `google-services.json` into `app/`. It's ignored by git.
4. Paste [`firestore.rules`](firestore.rules) into Firestore → Rules and publish.
5. Run `gradle assembleDebug`.

Release builds are signed in GitHub Actions ([`build.yml`](.github/workflows/build.yml)) using repository secrets: `GOOGLE_SERVICES_B64`, `KEYSTORE_B64` and `KEYSTORE_PASSWORD`.

## Licence

[MIT](LICENSE)
