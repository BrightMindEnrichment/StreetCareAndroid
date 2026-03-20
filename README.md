# StreetCare Android

An Android app for coordinating street outreach and community care, built by Bright Mind Enrichment.

- **Package:** `com.app.bmeapplication1`
- **Min SDK:** 31 (Android 12)
- **Target SDK:** 36
- **Architecture:** Single-activity (Jetpack Navigation)

---

## Table of Contents

1. [Project Setup](#project-setup)
2. [Build Commands](#build-commands)
3. [Architecture Overview](#architecture-overview)
4. [Key Features](#key-features)
5. [Firebase & Google Services](#firebase--google-services)
6. [CI/CD Workflow](#cicd-workflow)
7. [Deprecated Items](#deprecated-items)
8. [Contributing](#contributing)

---

## Project Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- A `local.properties` file in the project root (not committed) with:

```properties
API_KEY=<Google Maps API key>
API_KEY_PLACES=<Google Places API key>
INTERACTION_LOG_COLLECTION=<Firestore collection name, e.g. InteractionLogDev>
HELP_REQUEST_COLLECTION=<Firestore collection name, e.g. HelpRequestDev>
```

`API_KEY` and `API_KEY_PLACES` are injected into `BuildConfig` at build time via the `defaultConfig` block in `app/build.gradle`.

### Google Services

`app/google-services.json` is tracked in git (despite being in `.gitignore` — it was force-added). It contains OAuth client registrations for multiple package variants:

| Package | Purpose |
|---|---|
| `com.app.bmeapplication1` | Production app (Play Store) |
| `com.app.test.bmeapplication1` | Internal test variant |
| `com.example.bmeapplication1` | Example/dev variant |
| `org.brightmindenrichment.street_care` | Original package (legacy) |

If Google Sign-In stops working after a Play Store release, the Play Store app signing SHA-1 may not be registered. See [Firebase & Google Services](#firebase--google-services) for details.

---

## Build Commands

### Gradle Wrapper

The build commands require the Gradle wrapper (`gradlew`). The wrapper script is committed, but `gradle-wrapper.jar` is not. If it's missing, generate it first:

```bash
# Requires Gradle 8.9 installed locally (https://gradle.org/releases/)
gradle wrapper --gradle-version 8.9
```

Alternatively, extract `gradle-wrapper-main-8.9.jar` from the `gradle-8.9-bin.zip` distribution and place it at `gradle/wrapper/gradle-wrapper.jar`.

Once the wrapper is in place, all commands below use `./gradlew` (Mac/Linux) or `gradlew.bat` (Windows).

### Commands

```bash
# Debug build
./gradlew assembleDebug

# Release AAB (used by CI)
./gradlew bundleRelease

# Install debug APK on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.brightmindenrichment.street_care.ExampleUnitTest"

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

---

## Architecture Overview

Single-activity app (`MainActivity`) with Jetpack Navigation (bottom nav + nav graph `mobile_navigation`).

### Package Structure

| Package | Responsibility |
|---|---|
| `org.brightmindenrichment.street_care` | `MainActivity`, `MyApplication`, splash, profile screens |
| `.ui.home` | Home screen with slider, "How to" videos, "What to Give" |
| `.ui.community` | Events & help-requests feed, RSVP, add event/help-request |
| `.ui.visit` | Visit log wizard (multi-step forms), interaction logs |
| `.ui.user` | Auth (Firebase Email, Google, Facebook), profile edit, chapter membership |
| `.ui.chaptermembership` | Three-step chapter membership form |
| `.ui.widget` | Custom views: `StepProgressView`, `StepState`, `StepValidator` |
| `.YouTube` | YouTube playlist fetch and playback via Retrofit |
| `.notification` | FCM + WorkManager-based background notification worker |
| `.di` | Hilt `AppModule` (provides `EventsDatabase` singleton) |
| `.util` | `Extensions`, `Constants`, `UiState`, `DataStoreManager`, `Queries`, `FirestoreCollections`, `DateTimeUtil` |
| `org.brightmindenrichment.data.local` | Room `EventsDatabase` with `EventDao` |

### Key Architectural Patterns

**Dependency Injection** — Hilt (`@HiltAndroidApp` on `MyApplication`, `@AndroidEntryPoint` on `MainActivity`). `AppModule` provides the Room database as a singleton.

**Navigation** — Single `MainActivity` hosts a `NavHostFragment`. Bottom nav bar visibility is toggled per-destination inside `addOnDestinationChangedListener`. The "Visit" tab redirects unauthenticated users to `loginVisitLogFragment`.

**Data layer** — Firebase Firestore is the primary data store. A local Room database (`EventsDatabase`) caches the `events` collection. Firestore collection names are configured via `local.properties` and exposed through `util/FirestoreCollections.kt`.

**Notification strategy** — Dual-path:
- *Foreground*: `MainActivity.onResume` attaches a snapshot listener; `onPause` removes it.
- *Background*: `NotificationWorker` (a `@HiltWorker` `CoroutineWorker`) runs periodically every hour via `WorkManager`, briefly attaches a snapshot listener for 30 seconds, then removes it.

**State** — `UiState<T>` sealed class (`Loading`, `Success`, `Failure`) is used in ViewModels to expose UI state via LiveData.

**Feature Flags** — `FeatureFlagManager` singleton fetches from Firestore collection `FeatureFlags` at startup. Active flags:
- `clearFormOnWorkflowExit` — controls whether the IL form is discarded or saved on exit
- `showILDraftResumeDialog` — controls whether a resume dialog is shown when continuing a draft

### Firebase Collections

| Collection | Purpose |
|---|---|
| `events` | Community outreach events (also mirrored to Room) |
| `helpRequests` | Peer help requests with statuses: `NeedHelp`, `HelpOnTheWay`, `HelpReceived` |
| `users` | User profiles (profile image stored in Firebase Storage at `users/{uid}/profile.jpg`) |
| `pastOutreachEvents` | Historical events |
| `FeatureFlags` | Feature flag document `flags` with `{ flagKey: Boolean }` fields |

---

## Key Features

### Interaction Log (IL) Wizard

Multi-step form under `ui/visit/interaction_logs/`. Fragments: `InteractionQ1Fragment`–`InteractionQ7Fragment`.

All IL fragments extend `BaseILQuestionFragment` and implement the `StepValidator` interface. Each step tracks:
- `wasSkipped` — user clicked Skip
- `isTouched` — user returned via the progress bar
- `isCurrentStepValid()` — whether required fields are filled

**Progress bar** (`StepProgressView`) — 7-dot clickable widget. Completed steps are tappable; clicking a dot saves state to DataStore before navigating back.

**Draft persistence** — Form state is auto-saved to DataStore on every navigation (Next, Previous, Skip, progress bar). Drafts survive app kills and are restored on next launch.

**Timezone handling** — All timestamps stored as UTC in Firestore:
- IL Q1: uses `Calendar` with selected timezone → `Firestore.Timestamp`
- II Q1 & Q4: times stored as ZonedDateTime strings (`"14:30:00-05:00[America/New_York]"`) and converted to UTC at submission

**Submission** — `InteractionLogFormConsentFragment` performs an atomic Firestore batch write: one `InteractionLog` document + N `HelpRequest` documents.

### Individual Interaction (II) Sub-form

Sub-form inside IL under `ui/visit/interaction_logs/individual_interaction/`. Fragments: `IndividualInteractionQ1`–`Q4`.

All II fragments extend `BaseIIQuestionFragment`. Supports:
- Creating new interactions
- Editing existing interactions (tracked via `editingIndex` in `IndividualInteractionViewModel`)
- Dynamic header showing edit mode vs. new interaction number

### ViewModels

| ViewModel | Scope | Responsibility |
|---|---|---|
| `InteractionLogViewModel` | `activityViewModels` | IL form state, draft persistence, Firestore submission |
| `IndividualInteractionViewModel` | `activityViewModels` | II form state, committed interactions list |

---

## Firebase & Google Services

### Google Sign-In

Sign-in uses `CredentialManager` with `GoogleIdTokenCredential`. The flow:

```
LoginFragment → cardGoogle click
  → LoginLifeCycleObserver.fetchGoogleSignInCredentials()
    → CredentialManager.getCredential()  [uses default_web_client_id]
    → SHA-1 match found → GoogleIdTokenCredential → FirebaseAuth
    → SHA-1 not found  → NoCredentialException → "No Google account found" toast
```

`R.string.default_web_client_id` is auto-generated by the google-services Gradle plugin from `google-services.json` (resolves to the web/type-3 client).

**If Google Sign-In fails on Play Store builds:** The Play Store re-signs the APK with its own key. That SHA-1 must be registered in Firebase:
1. Google Play Console → Release → Setup → **App signing** → copy SHA-1
2. Firebase Console → Project Settings → Android app `com.app.bmeapplication1` → **Add fingerprint**
3. Download updated `google-services.json` → replace `app/google-services.json` → commit

### Required Google Cloud APIs

| API | Required | Used by |
|---|---|---|
| Maps SDK for Android | YES | `CommunityFragment`, `MapSelectorFragment`, `VisitLogDetailsFragment` |
| Places API | YES | `InteractionQ3Fragment`, `IndividualInteractionQ1`, `MapSelectorFragment`, `AddEventFragment` |
| Geocoding API | YES | `LocationUtils`, `CommunityFragment` |

The Places API key must have **Android apps** as its application restriction in Google Cloud Console, with entries for each package + signing SHA-1 combination.

---

## CI/CD Workflow

**File:** `.github/workflows/android-release.yml`
**Trigger:** Every push to `main`, or manually via `workflow_dispatch`

### What It Does

On every push to `main`, the workflow:

1. Checks out the repository using a deploy key (to allow pushing back to `main`)
2. Bumps `versionCode` (auto-increments by 1) and `versionName` (semantic patch by default)
3. Injects secrets into `local.properties`
4. Decodes the release keystore from base64
5. Builds and signs a release AAB (`./gradlew bundleRelease`)
6. Uploads the AAB to the **internal testing** track on Google Play
7. Commits the version bump back to `main` with `[skip ci]` to prevent re-triggering

### Version Bumping

- **Automatic (on push):** patch version incremented (e.g. `25.0.0` → `25.0.1`)
- **Manual (workflow_dispatch):** choose `patch`, `minor`, or `major` bump, or provide a specific version/version code

The version bump commit uses `[skip ci]` in the message — GitHub natively suppresses the `push` trigger for that commit, preventing an infinite loop.

### Secrets Required

| Secret | Purpose |
|---|---|
| `STREETCARE_ANDROID_DEPLOY_KEY` | SSH private key for pushing version bump back to `main` |
| `API_KEY` | Google Maps API key |
| `API_KEY_PLACES` | Google Places API key |
| `INTERACTION_LOG_COLLECTION` | Firestore collection name for interaction logs |
| `HELP_REQUEST_COLLECTION` | Firestore collection name for help requests |
| `SIGNING_KEY_STORE_BASE64` | Release keystore (`.jks`), base64-encoded |
| `SIGNING_KEY_ALIAS` | Keystore alias |
| `SIGNING_KEY_PASSWORD` | Key password |
| `SIGNING_STORE_PASSWORD` | Keystore store password |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Google Play service account JSON for upload |

### Deploy Key Setup

The workflow uses an SSH deploy key (not `GITHUB_TOKEN`) to push the version bump commit because `GITHUB_TOKEN` cannot bypass branch ruleset protections.

To set up or rotate the deploy key:

```bash
# Generate a new key pair (no passphrase)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy_key -N ""
```

- Add `deploy_key.pub` (public key) to: **Repo → Settings → Deploy keys** with **Allow write access** checked
- Add `deploy_key` (private key) as secret `STREETCARE_ANDROID_DEPLOY_KEY`: **Repo → Settings → Secrets and variables → Actions**
- The deploy key must be in the ruleset **Bypass list**: **Repo → Settings → Rules → Rulesets → default branch**
- Do not commit either key file

### Manual Release

To trigger a minor or major version bump without waiting for a code push:

> **GitHub → Actions → Android CI/CD → Run workflow** → select `minor` or `major`

---

## Deprecated Items

### Removed Fragments

| Fragment | Replaced By |
|---|---|
| `VisitForm7a.kt` | `InteractionQ7Fragment` (same layout `fragment_log_interaction_q7`) |
| `ConsentFragment.kt` | `InteractionLogFormConsentFragment` |

### Deprecated Layouts (pending deletion)

The following 11 full layouts were replaced by 2 shared containers + 11 content-only layouts as part of the IL/II refactoring. They should be deleted once all fragments are confirmed working:

**IL layouts (replaced by `fragment_il_question.xml` + `content_il_q*.xml`):**
- `fragment_log_interaction_q1.xml` through `fragment_log_interaction_q7.xml`

**II layouts (replaced by `fragment_ii_question.xml` + `content_ii_q*.xml`):**
- `fragment_individual_interaction_q1.xml` through `fragment_individual_interaction_q4.xml`

### Legacy Data Formats

**Individual Interaction timestamps (pre-fix):** Times were stored as bare `LocalTime` strings (e.g. `"14:30:00"`) without timezone context, causing incorrect UTC conversion. The current format is a ZonedDateTime string (e.g. `"14:30:00-05:00[America/New_York]"`). `parseTimezoneAwareTime()` in `DateTimeUtil.kt` includes a fallback for legacy strings.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branching strategy, PR guidelines, code style, testing requirements, and secrets handling.
