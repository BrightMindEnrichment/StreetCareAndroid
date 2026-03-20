# Contributing to StreetCare Android

Thank you for contributing to StreetCare Android. Please read this guide before submitting changes.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Branching Strategy](#branching-strategy)
3. [Making Changes](#making-changes)
4. [Pull Request Guidelines](#pull-request-guidelines)
5. [Code Style](#code-style)
6. [Testing](#testing)
7. [Secrets & Sensitive Files](#secrets--sensitive-files)

---

## Getting Started

1. Fork or clone the repository
2. Follow the [Project Setup](README.md#project-setup) section in the README to configure `local.properties` and the Gradle wrapper
3. Open the project in Android Studio and let Gradle sync

### Project Board

We track all work on the [GitHub Project Board](../../projects). Issues are available there — check it before starting work to avoid duplicating effort or picking up something already in progress. If you plan to work on an issue, leave a comment so it can be assigned to you.

---

## Branching Strategy

- `main` is the production branch — all CI releases are triggered from it
- Create feature branches off `main`:
  ```
  feature/<short-description>
  fix/<short-description>
  chore/<short-description>
  ```
- Do not push directly to `main` — branch protection requires a pull request with at least 1 approval

---

## Making Changes

- Keep changes focused — one feature or fix per PR
- Follow existing architecture patterns (see [Architecture Overview](README.md#architecture-overview)):
  - New screens should be fragments added to `mobile_navigation.xml`
  - ViewModel state should use `UiState<T>` (`Loading`, `Success`, `Failure`)
  - Firestore collection names must come from `FirestoreCollections.kt`, not hardcoded strings
  - UI strings belong in `strings.xml` — do not hardcode in Kotlin or XML layouts
- If adding a new Firestore collection, add it to `FirestoreCollections.kt` and document it in the README

---

## Pull Request Guidelines

- Target branch: `main`
- PR title should be short and descriptive (imperative mood: "Add X", "Fix Y", "Remove Z")
- Include a summary of what changed and why
- Link any related issues
- Resolve all conversations before requesting re-review
- At least 1 approval is required before merging
- Squash merging is the default — keep your commit history clean but don't worry about squashing manually

### PR Checklist

- [ ] `local.properties` is configured and the app builds locally
- [ ] No secrets, API keys, or keystore files committed
- [ ] New UI strings added to `strings.xml`
- [ ] Navigation graph updated if new fragments were added
- [ ] Tested on a physical device or emulator (API 31+)
- [ ] No regressions in existing IL/II form flows

---

## Code Style

- Kotlin only — no new Java files
- Follow existing naming conventions:
  - Fragments: `<Feature>Fragment.kt`
  - ViewModels: `<Feature>ViewModel.kt`
  - Layouts: `fragment_<feature>.xml`, `content_<feature>.xml` (content-only), `item_<feature>.xml` (list items)
- Prefer `lateinit var` for views initialized in `onContentViewCreated` / `onViewCreated`
- Use binding where already established; use `findViewById` only in fragments that predate binding adoption
- Do not add comments unless the logic is genuinely non-obvious

---

## Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

Manual testing is required for:
- IL wizard (Q1 → Q7), including skip, previous, and progress bar dot navigation
- II sub-form (Q1 → Q4), including edit and delete flows
- Draft persistence: fill form, kill app, reopen, verify state restored
- Google Sign-In on both debug and release builds
- Places Autocomplete in Q3 and II Q1

---

## Secrets & Sensitive Files

The following files must **never** be committed:

| File | Reason |
|---|---|
| `local.properties` | Contains API keys |
| `app/release-keystore.jks` | Release signing key |
| `deploy_key` / `deploy_key.pub` | CI deploy SSH key |

`app/google-services.json` is an exception — it is tracked in git despite being in `.gitignore` (force-added). If you update Firebase settings, download the new file and commit it. See [Firebase & Google Services](README.md#firebase--google-services) in the README.

CI secrets are managed in **GitHub → Settings → Secrets and variables → Actions**. See the [CI/CD Workflow](README.md#cicd-workflow) section of the README for the full secrets list.
