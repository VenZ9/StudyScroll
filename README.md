# StudyScroll

> **Native Android application that transforms chapter PDFs into a focused vertical feed of 30–60 source-grounded micro-lessons.**

[![Build StudyScroll Debug APK](https://github.com/VenZ9/StudyScroll/actions/workflows/android.yml/badge.svg)](https://github.com/VenZ9/StudyScroll/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

StudyScroll turns dense, overwhelming textbook chapters into concise, reviewable study units (definitions, key facts, ordered processes, comparisons, formulas, examples, and active-recall quizzes) while strictly preserving exact 1-based page numbers and verbatim source excerpts for every statement.

---

## 📱 Features

- **100% Offline Core**: PDF import, local text extraction, bounded size validation, template-based lesson generation, vertical feed, bookmarks, progress tracking, and narration all work offline without an internet connection or API key.
- **Strict 25 MiB PDF Limit**: Enforces an exact 25 MiB limit (26,214,400 bytes) with bounded chunk streaming to prevent memory exhaustion and out-of-memory errors on mobile devices.
- **Vertical Micro-Lesson Feed**: Distraction-free, full-viewport vertical swipe feed with editorial typography, subtle progress tracking (`8 of 42`), and persistent position restoration across app launches.
- **Source Traceability & Evidence Drawer**: Every micro-lesson cites its exact original PDF page number(s) (`Source: p. 14`) and includes a dedicated Source Evidence sheet with the supporting verbatim excerpt.
- **Active Recall Quizzes**: Interactive flashcard quizzes (multiple choice, recall, and True/False) generated directly from chapter concepts with immediate feedback and explanation.
- **Multi-Provider AI Architecture**:
  - **Offline Heuristic Engine** (Default, zero network, zero key)
  - **Groq** (Official OpenAI-compatible endpoint with fast Llama 3.3 models)
  - **Google Gemini** (Official generateContent API with Gemini 2.5 Flash / 1.5 Flash)
  - **NVIDIA NIM** (Official API with Llama 3.3 Instruct)
  - **Kilo AI Gateway** (Supports API key or documented anonymous access for eligible free-tier models, rate-limited by IP)
  - **Custom OpenAI-Compatible Endpoints** (User-configurable base URL & model ID)
- **Keystore-Backed AES-GCM Encryption**: User API keys are encrypted using Android Keystore-backed AES-256-GCM. Plaintext secrets are never stored in Room, DataStore, logs, or exports.
- **Hard Local-Only Mode**: A toggle in Settings that strictly blocks all cloud requests at the network layer in code.
- **Explicit Cloud Consent**: Bounded text excerpts (never the PDF itself) are sent to cloud providers only after explicit user confirmation.
- **Native Android Narration**: Integrated with Android's built-in `TextToSpeech` engine with adjustable playback rates (0.5x – 2.0x), strictly user-initiated.
- **Zero Telemetry**: No analytics, no advertising SDKs, no tracking, and no external application backend.

---

## 🏗️ Architecture & Technology Stack

- **Platform**: Native Android (Kotlin, Jetpack Compose, Material 3)
- **Minimum SDK**: Android 8.0 (API 26)
- **Target / Compile SDK**: Android 15 (API 35)
- **Toolchain**: AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1, Java 17
- **Database**: Room 2.6.1 (with KSP annotation processing)
- **Preferences**: Jetpack DataStore Preferences 1.1.1
- **Networking**: OkHttp 4.12.0 with Kotlinx Serialization 1.7.3
- **PDF Extraction**: Apache PDFBox for Android (`com.tom-roush:pdfbox-android:2.0.27.0`)
- **Security**: Android Keystore AES/GCM/NoPadding 256-bit authenticated encryption

---

## 🚀 Downloading and Installing the APK

### Direct Download via GitHub Actions (From Any Device / Phone)

1. Open the [StudyScroll GitHub Actions](https://github.com/VenZ9/StudyScroll/actions) page in your browser.
2. Tap on the latest completed workflow run (marked with a green checkmark `✓`).
3. Scroll down to the **Artifacts** section at the bottom of the summary page.
4. Tap **studyscroll-debug-apk** to download the zip archive containing `app-debug.apk`.
5. Extract the `.apk` file and tap it on your Android device to install.
   *(Note: You may need to allow "Install unknown apps" in your browser or file manager settings).*

---

## 🛠️ Building Locally

### Prerequisites
- JDK 17 or higher
- Android SDK (API 35 platforms and build-tools)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/VenZ9/StudyScroll.git
cd StudyScroll

# Grant execution rights to the Gradle wrapper
chmod +x gradlew

# Run all automated unit tests
./gradlew testDebugUnitTest

# Build the debug APK
./gradlew assembleDebug
```
The compiled debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Automated Testing

StudyScroll includes comprehensive automated tests covering critical business logic:
- **PDF Size Validation**: Tests strict boundary conditions (< 25 MiB accepted, == 25 MiB accepted, > 25 MiB rejected with exact error message, zero-byte empty file rejected, stream limit abortion).
- **Text Extraction & Segmentation**: Tests 1-based page number retention, heading detection, definition matching, formula extraction, comparison detection, and list parsing.
- **Offline Rule Engine**: Tests deterministic card generation, definition extraction, process mapping, active recall quiz generation, and empty block rejection.
- **AI Response Parsing**: Tests strict JSON schema parsing, Markdown fence stripping, valid page verification, and source excerpt confidence validation (`HIGH`, `MEDIUM`, `UNVERIFIED`).
- **Cryptographic Security**: Tests AES-GCM encryption/decryption roundtrips, ensures plaintext keys never appear in ciphertext blobs, key replacement, key deletion, and AEAD authentication tag validation on tampered payloads.
- **Local-Only & Cloud Consent Enforcement**: Tests that cloud providers are strictly blocked in code when local-only mode is active or when cloud consent has not been granted.

Run tests with:
```bash
./gradlew testDebugUnitTest --stacktrace
```

---

## 🔒 Privacy & Data Flow

- **What stays on your phone**: All imported PDF files, extracted text blocks, generated lessons, quiz attempt histories, bookmarks, and encrypted API keys remain exclusively on your device.
- **What is sent to cloud AI providers**: If (and only if) you choose a cloud provider and grant explicit consent, bounded extracted text blocks (relevant sentences/paragraphs) are sent to the selected provider to generate lessons. **Your raw PDF file is never uploaded.**
- **Offline Assurance**: You can toggle **Local-Only Mode** in Settings at any time to guarantee that all network capabilities are locked out in code.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
