# StudyScroll — Android Implementation Master Prompt

> **Use this entire document as the implementation brief for an AI coding agent.** Build the actual native Android project, run tests, fix failures, and configure GitHub Actions to produce a downloadable APK. Do not stop after generating a plan, mockup, or scaffold.

## 1. Product mission

Build **StudyScroll**, a native Android app that transforms a student's chapter PDF into a focused vertical feed of roughly **30–60 short, source-grounded micro-lessons**. The experience should feel like scrolling through concise study cards, not a social network. A lesson might explain a definition, fact, process, comparison, formula, example, diagram, misconception, or quick-recall question.

The core promise is: **turn a chapter into small, reviewable learning units while preserving where each claim came from.** The app must remain useful without an API key or internet connection. Cloud AI is optional, explicitly configured, and never the only way to generate lessons.

## 2. Non-negotiable constraints

- Native Android application only: **Kotlin + Jetpack Compose**. Do not build a website, WebView app, React Native app, Flutter app, or desktop companion.
- Offline-first. PDF import, local text extraction, basic lesson generation, feed navigation, bookmarks, progress, and saved lessons must work offline.
- Zero mandatory cost. No required subscription, paid service, account, backend, API key, or billing setup.
- Use free/open-source tooling where practical. Do not introduce a paid dependency or service as a requirement.
- Build APKs using **GitHub Actions**; assume the developer may not have a PC and may work from an Android phone.
- Keep the UI minimalist, calm, content-first, accessible, and low-distraction. No gradients, neon, decorative blobs, excessive glassmorphism, social features, or needless nested cards.
- Never hardcode developer-owned API keys or put keys in the repository.
- Do not invent content from the PDF. Keep source page references and a supporting excerpt for every generated lesson.
- Do not silently upload the PDF or silently switch providers. Explain cloud processing before the first upload/request and ask for consent.
- Implement real functionality. Do not leave fake buttons, placeholder provider integrations, TODO-only critical flows, or pretend-success screens.

## 3. Suggested stack and architecture

Use current stable versions that are mutually compatible at implementation time; pin versions and commit Gradle wrapper files.

- Kotlin, Android Gradle Plugin, Gradle wrapper, JDK supported by the chosen AGP.
- Jetpack Compose + Material 3, Navigation Compose, lifecycle/ViewModel, Kotlin coroutines and Flow.
- Room for persistent lessons, documents, bookmarks, progress, generation jobs, and provider metadata that is safe to store.
- DataStore for non-secret preferences.
- Android Storage Access Framework (SAF) document picker for PDF import. Prefer `ACTION_OPEN_DOCUMENT` / Compose Activity Result contracts; do not request broad storage permission merely to import one user-selected PDF.
- PDF extraction: choose a maintained Android-compatible library after verifying license, APK size, API compatibility, and whether it extracts text locally. Isolate it behind an interface so it can be replaced. Preserve one-based source page numbers. Do not upload PDFs to third-party PDF parsing services.
- Networking for cloud providers: a small, testable HTTP client such as Ktor or OkHttp with timeouts, bounded retries, cancellation, and provider-specific adapters. Use official documented endpoints and current model IDs only.
- API key encryption: Android Keystore-backed encryption using AES-GCM. Store ciphertext in app-private storage; never plaintext secrets in Room, DataStore, preferences, logs, analytics, backups, crash reports, exported files, or source control.
- Unit tests for core logic; Compose/UI tests where practical; instrumented tests only when supported by CI.
- No analytics SDK, ads SDK, telemetry, sign-in, or remote backend by default.

Recommended clean structure (adapt as needed, but preserve separation of concerns):

```text
app/
  src/main/java/<package>/
    app/                 # Application, navigation, theme
    core/                # Result/error types, dispatchers, utilities
    data/local/          # Room entities, DAOs, database
    data/security/       # Keystore and encrypted secret repository
    data/pdf/            # SAF URI handling, validation, extraction
    data/ai/             # Provider adapters, HTTP, model catalog
    data/repository/     # Repository implementations
    domain/model/        # Document, Lesson, SourceReference, Provider
    domain/usecase/      # Import, generate, validate, export, progress
    ui/home/
    ui/importpdf/
    ui/outline/
    ui/feed/
    ui/lesson/
    ui/quiz/
    ui/providers/
    ui/settings/
    ui/library/
    ui/components/
  src/test/
  src/androidTest/
.github/workflows/android.yml
README.md
LICENSE (choose a suitable license and state it accurately)
```

Choose a sensible application ID, e.g. `app.studyscroll`, and use it consistently. Set a reasonable minimum SDK (prefer API 26 unless a dependency or requirement justifies another). Set compile/target SDK to a current stable SDK supported by the selected toolchain. Avoid adding permissions unless a feature truly needs them.

## 4. User experience and screens

### 4.1 Design system

Visual direction:
- Light theme: warm/cool off-white background, charcoal text, restrained violet/indigo accent, subtle dividers and borders.
- Dark theme: equally restrained near-black background, comfortable light text, same restrained accent.
- Editorial typography with a clear distinction between title, lesson heading, body, labels, and source citation.
- Generous whitespace and readable line lengths. Strong contrast and touch targets around 48 dp.
- Use simple line icons, not emoji as app iconography.
- Motion is minimal, fast, and purposeful. Respect system animation settings and reduced-motion preferences where practical.
- Do not use gradients, glowing neon, floating blobs, decorative illustrations, glass panels, excessive shadows, or a card nested inside another card.
- The lesson itself should occupy roughly 80–90% of the useful viewport. Controls should not compete with the content.

### 4.2 Home / library

Show a simple title, a clear **Import PDF** primary action, recent documents, and small progress indicators. Empty state should explain the workflow in one or two sentences. No account, feed popularity, streak pressure, social counts, or intrusive gamification.

Each document row/card should show title, page count if known, generated lesson count, last opened date if useful, and progress. Include rename and delete actions with confirmation for destructive operations. Deleting a document should clearly explain whether its generated lessons and progress will also be deleted.

### 4.3 PDF import flow

- Open Android system document picker restricted to PDF MIME type where supported.
- Validate the chosen document’s size **before expensive parsing**. Limit is exactly **25 MiB = 26,214,400 bytes**.
- Accept sizes below the limit and exactly at the limit. Reject sizes above the limit.
- Error text: **“PDF exceeds the 25 MB limit. Choose a smaller file.”**
- Never read the entire PDF into an unbounded byte array. Stream or use a bounded copy to app-private cache if needed, checking actual bytes read because provider-reported size can be missing or inaccurate.
- Handle missing size, unreadable URI, permission loss, invalid MIME type, corrupt/encrypted PDFs, empty PDFs, password-protected PDFs, and scanned/image-only PDFs.
- Explain that scanned PDFs may need OCR and that OCR may not be available offline in the first version. Do not claim extraction succeeded when no usable text was found.
- Keep long-running import/generation cancellable and report meaningful progress. Do not freeze the UI thread.
- Store source URI access only when appropriate and persist URI permissions if required. Avoid copying PDFs unnecessarily. If caching a copy, delete it when no longer needed.

### 4.4 Extract and inspect chapter

Extract text locally and retain a mapping from each extracted block to its original one-based page number. Detect likely headings, paragraphs, lists, tables where feasible, formulas, and figure captions, but do not pretend to understand an image merely because it has a caption.

After extraction, show a review screen:
- PDF title and page count.
- Extracted text status and any warnings.
- Detected headings / chapter outline where possible.
- Lesson generation mode: **Offline (no key)**, **AI provider**, or **Local model** if a supported local model is implemented.
- If cloud mode is selected, show which provider/model will receive relevant extracted text, that the selected provider processes the excerpt, and that the PDF itself is not uploaded by default. Ask for explicit consent before the first cloud request.
- Let the user choose all pages or a page range if practical.
- Provide Generate lessons and Cancel actions.

### 4.5 Vertical lesson feed

Implement a native Compose vertical swipe feed, ideally using `VerticalPager` from a compatible Compose Foundation version or a robust lazy-list snap solution. It must support accessibility, touch scrolling, keyboard/DPAD where reasonable, and stable item identity.

A lesson may contain:
- Short title.
- One main learning point, definition, or question.
- Concise explanation (usually 1–3 short paragraphs or a compact list).
- Optional worked example, comparison, formula, steps, or simple diagram.
- A source line such as `Source: p. 12` or `Sources: pp. 12–13`.
- Optional “Why this matters” / memory cue only when useful and grounded.

Controls: bookmark, mark understood / review later, show source excerpt, previous/next or unobtrusive progress, and optional narration. A small quiz/reveal action is allowed when the lesson includes a quiz. Keep controls subtle and predictable. Avoid autoplay audio and infinite animation. Provide clear pause/resume if narration is active.

Show progress as a restrained line or `8 of 42`; do not create manipulative streak mechanics. Preserve current lesson position across process death and reopening. Avoid reordering lessons unexpectedly.

### 4.6 Lesson detail and source evidence

Every lesson must be traceable to its PDF page(s). Add a source drawer/sheet or detail area showing the supporting excerpt and original page number(s). If a generated statement cannot be linked to a source excerpt, flag it as unverified or discard it; do not present it as verified PDF content.

### 4.7 Quiz and active recall

Support lightweight questions based on extracted content: multiple choice, true/false, or short answer where answer checking is reliable. Store the question, answer, explanation, source references, and user result. Clearly distinguish a quiz generated from the chapter from official textbook exam questions. Never invent citations or claim that a question is from the textbook unless it actually is.

### 4.8 Library, bookmarks, and review

Provide document-level lesson feed, bookmarked lessons, and a simple “Review” list for lessons marked review later or answered incorrectly. Persist all state locally. Keep sorting/filtering small and useful.

### 4.9 Settings

Include:
- Theme: System / Light / Dark.
- Default generation mode: Offline / selected AI provider / Local model if implemented.
- Provider manager.
- Cloud consent and data-handling explanation.
- Narration enable/disable and speech rate where practical.
- Export/import app data where implemented, with a warning that secrets are never included.
- About, app version, licenses, and privacy statement.

## 5. Lesson-generation architecture

Separate source extraction, segmentation, generation, validation, and rendering. The UI must not contain provider-specific generation logic.

Create a domain model similar to:

```kotlin
data class SourceReference(
    val pageNumber: Int,
    val excerpt: String
)

data class Lesson(
    val id: String,
    val documentId: String,
    val orderIndex: Int,
    val type: LessonType,
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList(),
    val example: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val explanation: String? = null,
    val sources: List<SourceReference>,
    val confidence: ConfidenceLabel,
    val bookmarked: Boolean = false,
    val understood: Boolean? = null
)
```

Adapt the schema for Room serialization and migrations. Avoid storing large duplicate blobs in every row. Store lesson order and sources deterministically. Types may include `DEFINITION`, `KEY_FACT`, `PROCESS`, `COMPARISON`, `EXAMPLE`, `FORMULA`, `DIAGRAM_NOTE`, `QUIZ`, `SUMMARY`, and `VOCABULARY`.

### 5.1 Offline rule-based engine (required)

The app must always provide this path without network access or credentials. Build a deterministic, testable rule-based engine that:
1. Splits extracted text into page-aware blocks.
2. Detects likely headings and sections using typography/structure when available and text heuristics otherwise.
3. Segments paragraphs/lists into bounded, coherent chunks while preserving source pages.
4. Generates lessons with templates for definitions, key facts, ordered processes, comparisons, formulas, examples, and recall questions when the source supports them.
5. Produces a compact chapter overview and useful review prompts.
6. Removes exact/near duplicates, avoids empty/too-long cards, and retains source excerpts.
7. Does not turn arbitrary sentences into false definitions or fabricate missing answers.

Be honest about limitations: template mode may produce simpler cards than a capable language model. If the source is unsuitable or extraction fails, show a helpful error and allow the user to inspect extracted text; do not create 30–60 filler cards. The target is roughly 30–60 cards for a sufficiently long chapter, not a hard quota. It is better to produce fewer accurate cards than many repetitive ones.

### 5.2 Optional local model (only if feasible)

Local AI is optional. Do not make it a release blocker if it substantially increases APK size, complexity, or phone requirements. If implemented:
- Use a documented, legally compatible on-device inference runtime and a model with a clear license.
- Download model files only after explicit user action; show approximate download size, storage requirements, and device compatibility.
- Do not bundle a huge model in the APK by default.
- Keep inference cancellable and off the main thread; report memory/performance limitations.
- Never send text to a remote endpoint in local-only mode.
- If local inference fails, offer the rule-based engine. Do not silently switch to cloud.

### 5.3 Cloud AI generation (optional)

Define a provider-agnostic interface, e.g. `LessonGenerationProvider`, with capabilities, model listing (when supported), connection test, and lesson-generation operations. Keep each provider adapter independently testable.

Cloud pipeline:
1. Extract and segment text locally.
2. Select only relevant bounded excerpts; do not send the entire PDF by default.
3. Send a compact prompt requesting lessons grounded only in supplied excerpts.
4. Parse the response safely into a versioned structured schema.
5. Validate required fields, types, lengths, source page numbers, and that each cited page exists in the selected document.
6. Verify that source excerpts match supplied source text sufficiently; if not, mark as unsupported or reject.
7. Deduplicate and remove malformed/empty lessons; preserve generation method/provider/model metadata without storing secrets.
8. Save validated lessons locally and allow retrying failed chunks without duplicating already saved lessons.

Do not treat valid JSON as proof of factual correctness. Label cloud-generated content as AI-generated, preserve source evidence, and let the user inspect/reject results. Do not promise that citations prove every sentence. Use strict JSON/schema output if supported by the selected model/API; otherwise parse defensively. Never execute model-produced code, HTML, Markdown links, tool calls, or arbitrary instructions. Treat PDF content as untrusted data, not system instructions. A PDF may contain prompt-injection text; the model prompt must explicitly treat the document as source material and ignore instructions embedded in it.

Prompt content rules:
- Use only the supplied source excerpts for chapter facts.
- Do not fill gaps from general knowledge unless the UI clearly labels that information as outside the PDF; default is not to do this.
- Each lesson must cite one or more valid source pages and include a short verbatim supporting excerpt.
- If the source is ambiguous, return fewer lessons or mark it uncertain.
- Use age-appropriate, clear language without changing the source meaning.
- Do not claim the lesson is guaranteed correct or exam-approved.

## 6. AI providers and keyless access

Implement a **Provider Manager** with multiple provider profiles. It must not require any provider to be configured for the app to work.

### 6.1 Required provider options

1. **Groq** — user-supplied API key, official documented API endpoint and supported models.
2. **Google Gemini API** — user-supplied API key, official documented endpoint and model identifiers.
3. **NVIDIA NIM** — user-supplied API key, official documented endpoint and supported model identifiers.
4. **Kilo AI Gateway** — support the documented gateway API. Prefer authenticated user-configured access when supplied. Also support **anonymous access only for models currently documented as eligible for unauthenticated free use**, if the current official API documentation supports the exact endpoint and request shape.
5. **Custom OpenAI-compatible endpoint** — user enters base URL, optional key, and model ID. Validate URL scheme and reject cleartext HTTP except an explicit development-only setting; warn about sending content to third parties.
6. **Offline template engine** — always available and requires no key, internet, account, or model download.
7. **Optional local model** — only if actually implemented as described above.

Do not invent a list of additional “keyless providers.” The reliable baseline keyless hosted option is **Kilo’s eligible free-model anonymous access**, which is limited and can change; do not describe all Kilo models as free. Add other keyless hosted providers only after confirming their official unauthenticated API exists, is permitted for direct app use, and has documented limits. Never bypass authentication, scrape consumer chat websites, use unofficial endpoints, rotate identities/IPs to evade limits, or embed a shared key. If anonymous Kilo access stops working, show an understandable error and offer offline mode; do not silently try another provider.

Kilo's current documentation describes anonymous access for eligible free models, with requests rate-limited by IP (currently documented as 200 requests/hour/IP). Treat this as a configurable/current-policy detail, not a permanent guarantee. Some free-model routing may send prompts to upstream providers with their own data-handling policies; clearly warn users not to submit personal or confidential content through such services. Fetch the live model catalog when supported or allow manual model selection, and do not hardcode a supposedly permanent free-model list.

### 6.2 Provider settings UI

Build a restrained provider settings screen with:
- Provider list and connection status (unknown / testing / available / error), without exposing secrets.
- Add provider, edit configuration, test connection, choose model, set as preferred, remove provider.
- Provider type, display name, base URL when applicable, model ID, auth mode (API key / documented anonymous / none for offline), last test result, and optional context/output limits if needed.
- API key input obscured by default, with an explicit show/hide toggle.
- Save/cancel controls and clear validation messages.
- Per-provider data disclosure and rate-limit notes.
- A warning that direct API keys stored on a user device can still be exposed on a rooted/compromised device or through reverse engineering; local encryption protects stored secrets but cannot make client-side secrets impossible to extract.

Never show the full key again after saving. Offer replace-key and delete-key actions. Avoid copying secrets to clipboard. Ensure error messages never include authorization headers, request bodies containing secrets, or full keys.

### 6.3 Secret storage

- Generate or use an AES-GCM data-encryption key protected by Android Keystore.
- Encrypt each API key before persistence. Use a well-defined versioned encrypted record format with nonce/IV and authentication tag; never reuse a nonce with the same key.
- Keep ciphertext and non-secret provider metadata in app-private storage. Never put plaintext keys in Room, DataStore, regular SharedPreferences, logs, analytics, crash reports, exported backups, test snapshots, or source control.
- Do not back up encrypted credentials unless the implementation can guarantee the Keystore key and encrypted data remain safely paired; safest default is exclude secrets from backup.
- Delete ciphertext on provider removal and handle Keystore invalidation with a message asking the user to re-enter keys.
- Add tests proving plaintext keys are not persisted in ordinary app data records and do not appear in logs.
- Never put API keys in BuildConfig, Gradle files, GitHub Actions secrets used to ship a public APK, assets, resources, or default settings.

### 6.4 Model catalog and request behavior

- Use each provider’s official documented API format. Do not assume Gemini or NVIDIA NIM uses exactly the same URL, headers, or model list behavior as Groq.
- For OpenAI-compatible providers, isolate base URL and path composition and avoid malformed duplicate `/v1` paths.
- Fetch model lists only from documented endpoints. If unavailable, accept a manually entered model ID and explain that availability must be tested.
- Test Connection should make a minimal, bounded request appropriate to the provider. A successful HTTP status alone may not mean a model can generate lessons; display the real response outcome.
- Handle 400, 401, 403, 404, 408, 413, 429, 5xx, offline, TLS, DNS, timeout, malformed JSON, empty response, unsupported model, and cancellation. Give concise actionable messages.
- Use short bounded retries only for transient failures; respect `Retry-After` where provided. Do not endlessly retry or repeat billable requests blindly.
- Show a confirmation when the chosen provider is not keyless/free or when its pricing is unknown. Do not assert that a model is free unless the current official model catalog clearly identifies it as free.
- No silent fallback from a selected provider to another cloud provider. If fallback is enabled by the user, show the fallback order and ask permission before the first use; never send content to a new provider without explicit consent.
- Add a **Local only** toggle that disables all cloud requests in code, not just the UI. Test this guarantee.
- Never upload the source PDF by default; send bounded extracted text excerpts only.

## 7. Data model and persistence

Use Room entities and migrations for:
- `DocumentEntity`: ID, display title, source metadata, page count, import/extraction status, timestamps, generation status, and current feed position.
- `PageTextEntity` or equivalent page-aware block store: document ID, one-based page number, block order, block type, text.
- `LessonEntity`: stable ID, document ID, order index, lesson type, text fields, source references/excerpts, generation mode/provider/model metadata, bookmark, review status, timestamps.
- `QuizAttemptEntity`: question/lesson ID, answer, correctness if determinable, and timestamp.
- `GenerationJobEntity`: status, progress, chunk index, recoverable error, cancellation status. Do not store secret request headers or raw keys.
- Provider metadata only: ID, provider type, label, base URL, model ID, auth mode, enabled flag, timestamps. Secrets belong in the encrypted secret store, not Room.

Ensure foreign keys/cascades are intentional. Add database migrations instead of destructive migration in release builds. Use stable IDs and transactions so interrupted generation does not create duplicate lessons. Store the page index one-based to match normal PDF viewers.

## 8. Narration and accessibility

- Optional Android `TextToSpeech`; do not require a cloud voice service.
- Initialize and release TextToSpeech safely; handle unavailable languages and initialization errors.
- Narration is user-initiated, never autoplayed. Provide play/pause/stop and do not read UI controls/source citations unnecessarily.
- Accessibility semantics for pager/cards, meaningful labels for icons, scalable fonts, sufficient contrast, and TalkBack-compatible navigation.
- Respect system font scale and avoid clipping with large text. Test at multiple font scales and small screens.

## 9. Export, privacy, and data deletion

- If export is implemented, use SAF `ACTION_CREATE_DOCUMENT` and export documents/lessons/bookmarks in a documented, versioned JSON format. Optionally export plain-text/Markdown lessons.
- **Never include API keys, encrypted key blobs, authorization headers, or provider secrets in exports.**
- Validate imports; enforce size/structure limits and schema versions. Treat imported content as untrusted. Avoid arbitrary path writes.
- Provide delete-document and delete-all-local-data actions with confirmation and accurate scope. Remove cached PDF copies and generated data according to the selected action.
- Privacy statement must clearly explain: what stays local; what is sent to a selected cloud provider; that excerpts are transmitted only after consent; that provider data practices and retention vary; and how to use offline mode.
- No analytics, ads, trackers, or background uploads. Do not add a backend just for telemetry.

## 10. PDF correctness and error handling

Test the size validator at:
- 25 MiB minus 1 byte: accept.
- Exactly 26,214,400 bytes: accept.
- 25 MiB plus 1 byte: reject.
- Unknown provider-reported size: bounded stream check before parsing.
- Actual stream larger than claimed size: reject as soon as the limit is exceeded.

The limit is for the PDF file itself. Do not confuse decimal 25,000,000 bytes with 25 MiB. Do not load an over-limit PDF into memory before rejecting it.

Also test:
- Non-PDF file renamed `.pdf`.
- Corrupt PDF.
- Empty PDF.
- Password-protected PDF.
- Text-based PDF with multiple pages.
- Image-only/scanned PDF.
- PDF with unusual Unicode, equations, bullets, tables, and line breaks.
- Huge page count or pathological text causing excessive work.
- URI access revoked or provider unavailable during extraction.
- Cancellation while copying/extracting.

Keep memory and CPU use bounded. Run parsing on an IO dispatcher or dedicated executor. Avoid logging extracted sensitive content. If PDF text is extremely long, process in page-aware chunks.

## 11. Testing requirements

Create meaningful automated tests; do not only make the project compile.

### Unit tests
- PDF size validator boundary tests (below, equal, above, unknown, false reported size).
- Page-number preservation and block segmentation.
- Heading/paragraph/list heuristics.
- Lesson template generation for definitions, facts, steps, comparisons, formulas, and quizzes.
- Empty/short/duplicate/unsupported lesson rejection.
- Source reference validity and excerpt matching.
- JSON parsing, schema validation, malformed provider response handling.
- Provider URL composition and authorization header construction without exposing secrets.
- Encrypted key round-trip, replacement, deletion, and Keystore invalidation handling where testable.
- Local-only mode guarantees no network provider is invoked.
- Generation retry/resume does not duplicate lessons.
- Room DAO persistence and migrations.

### UI/integration tests
- Empty home state and PDF picker launch.
- Import success/error states.
- Provider add/edit/test/remove flow.
- API key masked by default and not shown after saving.
- Lesson feed swipe/navigation, bookmarking, source sheet, review state, and restoration.
- Dark/light/system theme and large font scaling.
- Cancellation and network error feedback.

Use fake/mock HTTP engines for tests; tests must not require real API keys, external network, or paid services. Do not commit real secrets into fixtures.

## 12. GitHub Actions and APK release

Create `.github/workflows/android.yml` that:
- Triggers on push, pull request, and manual dispatch.
- Checks out repository.
- Sets up the chosen JDK and Gradle caching.
- Runs formatting/lint checks available in the project, unit tests, and Android build.
- Builds a debug APK on normal CI runs.
- Uploads APK and test reports as workflow artifacts with stable names.
- Optionally builds a release APK only when release configuration is supplied securely. Never commit keystore files or passwords.
- Does not require paid services or secrets just to build a debug APK.
- Fails on compile/test/lint errors rather than swallowing failures.

Provide README instructions for building locally and through GitHub Actions from a phone. Explain where to download the APK artifact in the Actions run. Keep CI compatible with GitHub-hosted runners and avoid requiring a self-hosted runner.

For release signing, document the safe process for adding signing secrets to repository Actions secrets. Never print secrets to logs. Do not pretend a debug APK is a signed production release.

## 13. Performance and reliability

- Never do PDF parsing, database operations, model inference, or network requests on the main thread.
- Use coroutines with cancellation propagation. Closing a document or cancelling generation should stop unnecessary work.
- Use pagination/lazy rendering for large feeds and avoid eagerly composing all 60 cards.
- Keep database transactions short and batch inserts when appropriate.
- Handle process death and app restart during generation. Show a resumable state or a recoverable failure; do not leave a spinner forever.
- Cache extracted text and lessons locally so reopening does not regenerate everything.
- Prevent duplicate import/generation jobs for the same document unless user explicitly asks to regenerate.
- Provide a regenerate action with clear behavior: retain or replace prior lessons, never create accidental duplicates.
- Avoid excessive dependencies and APK bloat. Measure APK size and document any large optional model downloads separately.

## 14. Definition of done

The task is complete only when all of the following are true:

1. A complete Android Studio/Gradle project exists in the repository.
2. App launches to a functional home/library screen.
3. User can import a PDF using Android’s document picker.
4. The 25 MiB size limit is checked correctly, including exact boundary cases, before expensive processing.
5. PDF text is extracted locally with one-based page references, and extraction failures are explained.
6. Offline template mode generates usable, non-filler, source-linked lessons with no API key or network.
7. The vertical feed supports swiping, progress restoration, bookmarks, source evidence, and review state.
8. Provider Manager supports Groq, Gemini, NVIDIA NIM, Kilo Gateway (including eligible documented anonymous free models where currently supported), and a custom OpenAI-compatible endpoint.
9. API keys are added by the user, masked, encrypted with Android Keystore-backed AES-GCM, and never logged/exported.
10. No cloud request occurs before explicit cloud consent, and local-only mode is enforced in the networking layer.
11. No silent provider fallback, no embedded/shared API key, no hardcoded claims that a model is permanently free.
12. Tests cover size validation, source traceability, generation validation, secret handling, provider errors, and local-only behavior.
13. GitHub Actions builds the debug APK and publishes it as an artifact without requiring secrets.
14. README includes setup, build, CI artifact download, provider setup, privacy/data flow, known limitations, and testing instructions.
15. No fake functionality, critical TODOs, broken imports, unimplemented buttons, or known failing tests remain.

## 15. Execution instructions for the coding agent

Work directly in the current repository if one exists. Inspect the repository first; preserve useful existing code and do not overwrite user work blindly. If no project exists, initialize the native Android project and create the required files.

Then execute in this order:

1. Inspect repository, toolchain, and current build status.
2. Write a concise implementation checklist in the project README or issue notes, then begin implementation immediately.
3. Implement Gradle/project setup, app navigation, theme, Room, and core models.
4. Implement PDF picker, strict bounded size validation, local extraction, and tests.
5. Implement offline lesson engine and source validation first; make this path fully usable.
6. Implement lesson feed, bookmarks, progress restoration, quiz/review, and narration.
7. Implement encrypted provider secrets and provider settings UI.
8. Implement and test each provider adapter separately, using mock HTTP tests and current official documentation.
9. Add export/privacy flows, error states, and accessibility.
10. Configure GitHub Actions, README, licenses, and release notes.
11. Run all available tests and builds. Fix actual errors and rerun until green.
12. Inspect the final diff for secrets, unimplemented controls, unsafe permissions, debug-only assumptions, and accidental large assets.
13. Report exactly what was implemented, commands/tests run, build status, APK artifact path/link (if the platform provides it), and any genuine remaining limitations. Never claim a build passed unless it actually passed.

When requirements conflict, prioritize: data privacy and source accuracy; offline functionality; native Android correctness; PDF size safety; functional core learning flow; provider integrations; visual polish. Make reasonable implementation choices without asking for approval for every minor detail. If a requested capability cannot be safely or reliably completed, implement a truthful fallback and document the limitation rather than faking it.

## 16. Official documentation to consult during implementation

Use current official docs because API endpoints, models, rate limits, and Android toolchain requirements can change:

- Android Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
- Groq API documentation: https://console.groq.com/docs/openai
- Gemini API key documentation: https://ai.google.dev/gemini-api/docs/api-key
- NVIDIA NIM API quickstart: https://docs.api.nvidia.com/nim/docs/api-quickstart
- Kilo Gateway authentication: https://kilo.ai/docs/gateway/authentication
- Kilo Gateway usage and billing: https://kilo.ai/docs/gateway/usage-and-billing
- Kilo Gateway models: https://github.com/Kilo-Org/kilocode/blob/main/packages/kilo-docs/pages/gateway/models-and-providers.md

Do not copy old example model IDs blindly. Verify each endpoint, model, authentication mode, price/free status, rate limit, and data policy against the provider’s current official documentation at implementation time.
