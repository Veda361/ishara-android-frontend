# Ishaara Android — Phase 00 Project Audit

**Document Version**: 1.0.0  
**Audit Date**: September 21, 2026  
**Auditor Role**: Senior Android Architect & Production Code Auditor  
**Project Path**: `/home/dev/Desktop/ishara-frontend`  
**Audit Scope**: Read-Only Baseline Inspection (Phase 00)  

---

## 1. Executive Summary

The **Ishaara** Android project is currently an initial baseline Android application project scaffolded using Android Studio and Android Gradle Plugin (AGP) 9.4.1. The codebase is **100% Kotlin** utilizing modern **Jetpack Compose** with **Material 3** for UI rendering.

At present, the project is a clean development template:
- It contains **1 Activity** (`MainActivity.kt`) displaying a default "Hello Android!" scaffold.
- It contains **no business logic**, **no networking client**, **no authentication layer**, **no local database**, **no location or mapping SDKs**, **no push notification integration**, and **no navigation architecture**.
- However, rich **brand design assets** (including primary logos, dark/light variants, bus vehicle imagery, and motion videos) already exist in the repository's `assets/` and `app/src/main/res/drawable/` directories.
- The project is configured with a forward-looking SDK baseline (`minSdk 24`, `targetSdk 37`, `compileSdk 37`, Kotlin 2.2.10, Compose BOM 2026.02.01).

Because no application domain code has been written yet, there is no legacy debt or architectural anti-pattern to refactor. The codebase is in a greenfield state ready for a structured, modular production architecture implementing the two target personas: **Student / User** and **Driver / Conductor**.

---

## 2. Project Structure

The project is structured as a single-module Gradle build (`:app`).

```
ishara-frontend/
├── .gitignore
├── build.gradle.kts                     # Top-level plugin declarations
├── settings.gradle.kts                  # Repositories and module inclusion (:app)
├── gradle.properties                    # JVM arguments & config cache flags
├── gradlew / gradlew.bat                # Gradle wrapper scripts
├── local.properties                     # Local SDK directory pointer
├── docs/
│   └── API_REFERENCE.md                 # Complete Ishaara Backend API reference
├── gradle/
│   ├── libs.versions.toml               # Version catalog
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties    # Gradle 9.6.0 distribution
├── assets/                              # Brand & visual assets (repository root)
│   ├── logo-forming.mp4
│   ├── images/                          # 13 branding and vehicle image files
│   └── videos/                          # 4 bus profile/perspective MP4 videos
└── app/
    ├── build.gradle.kts                 # Application module configuration
    ├── .gitignore
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml      # Single exported MainActivity; no permissions
        │   ├── keepRules/
        │   │   └── rules.keep           # R8 keep rules placeholder
        │   ├── assets/
        │   │   ├── images/
        │   │   │   └── ishara_bus.jpg
        │   │   └── videos/
        │   │       └── .gitkeep
        │   ├── java/com/ishara/app/
        │   │   ├── MainActivity.kt      # Main entry point Composable
        │   │   └── ui/theme/
        │   │       ├── Color.kt         # Default Material3 color definitions
        │   │       ├── Theme.kt         # IsharaTheme definition (dynamic color enabled)
        │   │       └── Type.kt          # Material3 Typography baseline
        │   └── res/
        │       ├── drawable/
        │       │   ├── ic_launcher_background.xml
        │       │   ├── ic_launcher_foreground.xml
        │       │   └── ishara_bus.jpg
        │       ├── mipmap-*/            # Launcher icons (anydpi, hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi)
        │       ├── values/
        │       │   ├── colors.xml       # Legacy XML colors
        │       │   ├── strings.xml      # app_name = "ishara"
        │       │   └── themes.xml       # Theme.Ishara XML style
        │       └── xml/
        │           ├── backup_rules.xml
        │           └── data_extraction_rules.xml
        ├── test/java/com/ishara/app/
        │   └── ExampleUnitTest.kt       # Default JUnit4 unit test (2+2=4)
        └── androidTest/java/com/ishara/app/
            └── ExampleInstrumentedTest.kt # Default instrumented test (packageName assert)
```

---

## 3. Language & UI Technology

### Language
- **Kotlin files count**: 6 (4 in `main`, 1 in `test`, 1 in `androidTest`)
- **Java files count**: 0
- **Primary language**: **Kotlin** (100%)
- **Mixed-language areas**: None. Zero Java source files.

### UI Technology
- **UI Technology**: **Pure Jetpack Compose**
- **XML Layouts**: **None** (`res/layout` directory does not exist).
- **Compose Version**: Defined via BOM `2026.02.01` (`libs.versions.toml:L10`).
- **Compose Compiler**: Kotlin Compose Compiler plugin `2.2.10` (`org.jetbrains.kotlin.plugin.compose`).
- **Material Version**: **Material 3** (`androidx.compose.material3:material3` via BOM).
- **Activity Binding**: `androidx.activity:activity-compose:1.13.0`.
- **Edge-to-Edge**: Configured via `enableEdgeToEdge()` in [MainActivity.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt#L19).

---

## 4. Package & Application Identity

- **Namespace**: `com.ishara.app` ([app/build.gradle.kts:L7](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L7))
- **Application ID**: `com.ishara.app` ([app/build.gradle.kts:L13](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L13))
- **Package Declaration**: `package com.ishara.app` ([MainActivity.kt:L1](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt#L1))
- **Consistency**: The `namespace` and `applicationId` are identical and match the source directory structure `com/ishara/app`.
- **Product Flavors**: None defined.
- **Build Variants**: `debug` and `release`.

---

## 5. Android SDK / Gradle Configuration

- **compileSdk**: `37` (`version = release(37)` in [app/build.gradle.kts:L8-L10](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L8-L10))
- **targetSdk**: `37` ([app/build.gradle.kts:L15](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L15))
- **minSdk**: `24` ([app/build.gradle.kts:L14](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L14))
- **buildToolsVersion**: Default derived from AGP (not explicitly configured).
- **Android Gradle Plugin (AGP)**: `9.4.1` ([gradle/libs.versions.toml:L2](file:///home/dev/Desktop/ishara-frontend/gradle/libs.versions.toml#L2))
- **Gradle Version**: `9.6.0` ([gradle/wrapper/gradle-wrapper.properties:L5](file:///home/dev/Desktop/ishara-frontend/gradle/wrapper/gradle-wrapper.properties#L5))
- **Kotlin Version**: `2.2.10` ([gradle/libs.versions.toml:L9](file:///home/dev/Desktop/ishara-frontend/gradle/libs.versions.toml#L9))
- **JVM Target Compatibility**: `JavaVersion.VERSION_11` ([app/build.gradle.kts:L29-L32](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L29-L32))
- **Compose Compiler Configuration**: Managed by `org.jetbrains.kotlin.plugin.compose` version `2.2.10`.
- **Configuration Cache**: Explicitly enabled `org.gradle.configuration-cache=true` in [gradle.properties:L17](file:///home/dev/Desktop/ishara-frontend/gradle.properties#L17).

---

## 6. Dependencies

### Version Catalog: `gradle/libs.versions.toml`

| Category | Library Identifier | Version / Source | Apparent Purpose | In Source Code |
| :--- | :--- | :--- | :--- | :--- |
| **CORE** | `androidx.core:core-ktx` | `1.19.0` | Kotlin extensions for Android framework | Yes (scaffold) |
| **UI** | `androidx.compose:compose-bom` | `2026.02.01` | Jetpack Compose dependency bill of materials | Yes |
| **UI** | `androidx.activity:activity-compose` | `1.13.0` | Compose bridge for ComponentActivity | Yes (`MainActivity.kt`) |
| **UI** | `androidx.compose.material3:material3` | BOM managed | Material Design 3 UI components | Yes (`IsharaTheme`, `Scaffold`, `Text`) |
| **UI** | `androidx.compose.ui:ui` | BOM managed | Core Compose UI runtime | Yes |
| **UI** | `androidx.compose.ui:ui-graphics` | BOM managed | Graphics and rendering | Yes |
| **UI** | `androidx.compose.ui:ui-tooling-preview` | BOM managed | Compose layout previewing | Yes (`GreetingPreview`) |
| **UI (Debug)** | `androidx.compose.ui:ui-tooling` | BOM managed | IDE Compose inspection tools | Debug only |
| **UI (Debug)** | `androidx.compose.ui:ui-test-manifest` | BOM managed | Compose testing manifest hook | Debug only |
| **LIFECYCLE** | `androidx.lifecycle:lifecycle-runtime-ktx` | `2.11.0` | Lifecycle scopes and Flow utilities | Declared |
| **TESTING** | `junit:junit` | `4.13.2` | Local JVM unit testing | Yes (`ExampleUnitTest.kt`) |
| **TESTING** | `androidx.test.ext:junit` | `1.3.0` | AndroidJUnit4 runner | Yes (`ExampleInstrumentedTest.kt`) |
| **TESTING** | `androidx.test.espresso:espresso-core` | `3.7.0` | Instrumentation UI testing | Declared |
| **TESTING** | `androidx.compose.ui:ui-test-junit4` | BOM managed | Compose UI test runner | Declared |

### Missing Production Dependencies (NOT PRESENT IN CURRENT ANDROID PROJECT)
- **NETWORKING**: Retrofit, OkHttp, Ktor, Volley (None)
- **SERIALIZATION**: kotlinx.serialization, Moshi, Gson (None)
- **LOCAL STORAGE / DATABASE**: Room, SQLite, DataStore, SharedPreferences (None)
- **NAVIGATION**: Navigation Compose (`androidx.navigation:navigation-compose`) (None)
- **DEPENDENCY INJECTION**: Hilt, Koin (None)
- **LOCATION**: Google Play Services Location (`play-services-location`) (None)
- **MAPS**: Google Maps Compose, Mapbox, OSMDroid (None)
- **AUTHENTICATION**: Google Credential Manager, Google Sign-In SDK (None)
- **FIREBASE / PUSH**: Google Services Plugin, Firebase Messaging (FCM) (None)
- **REALTIME / WEBSOCKETS**: OkHttp WebSocket, Ktor WebSocket, Socket.IO (None)
- **AUDIO / MEDIA**: Media3, ExoPlayer, SpeechRecognizer (None)
- **IMAGE LOADING**: Coil, Glide (None)

---

## 7. Build Configuration

- **Build Types**:
  - `debug`: Standard debug configuration.
  - `release`: Configured with `optimization { enable = false }` ([app/build.gradle.kts:L24-L26](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L24-L26)).
- **Minification / R8**: Disabled for release (`enable = false`). ProGuard / R8 rules file is located at `app/src/main/keepRules/rules.keep` but currently commented out.
- **Resource Shrinking**: Disabled (not declared).
- **Signing Configuration**: None defined. Release builds are unsigned.
- **BuildConfig**: `buildConfig = true` is not enabled in `buildFeatures`.
- **Packaging Options**: Default.
- **Evaluation**: The current build configuration is a **development-only prototype setup**. It is **not production-ready** because optimization is explicitly disabled, no release keystore is wired, and no environment variable injection mechanism is present.

---

## 8. AndroidManifest Audit

**File**: [app/src/main/AndroidManifest.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/AndroidManifest.xml)

### Application Configuration
- **Application Class**: Default (`android.app.Application`; no custom Application class declared).
- **Application Label**: `@string/app_name` (`"ishara"`).
- **Icon**: `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.
- **Theme**: `@style/Theme.Ishara` (points to `android:Theme.Material.Light.NoActionBar` in [res/values/themes.xml:L4](file:///home/dev/Desktop/ishara-frontend/app/src/main/res/values/themes.xml#L4)).
- **Backup Configuration**: `android:allowBackup="true"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:fullBackupContent="@xml/backup_rules"`.
- **Network Security Configuration**: None declared (defaults to system TLS; cleartext HTTP blocked by default on API 28+).

### Components
- **Activities**:
  - `.MainActivity`:
    - `exported="true"`
    - `windowSoftInputMode="adjustResize"`
    - Intent Filter: `android.intent.action.MAIN` + `android.intent.category.LAUNCHER`.
- **Services**: **0** declared (no foreground services, no location services).
- **Receivers**: **0** declared.
- **Providers**: **0** declared.

### Permissions Audit
| Permission | Declared Status | Target Production Requirement |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | **NOT PRESENT** | Critical: Required for API & WebSockets |
| `android.permission.ACCESS_NETWORK_STATE` | **NOT PRESENT** | Required for connectivity observation |
| `android.permission.ACCESS_FINE_LOCATION` | **NOT PRESENT** | Critical: Required for GPS tracking & discovery |
| `android.permission.ACCESS_COARSE_LOCATION` | **NOT PRESENT** | Critical: Required for location fallback |
| `android.permission.ACCESS_BACKGROUND_LOCATION` | **NOT PRESENT** | Required for driver background tracking |
| `android.permission.FOREGROUND_SERVICE` | **NOT PRESENT** | Required for driver live route tracking |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | **NOT PRESENT** | Required for Android 14+ location foreground service |
| `android.permission.POST_NOTIFICATIONS` | **NOT PRESENT** | Required for Android 13+ push/ride notifications |
| `android.permission.RECORD_AUDIO` | **NOT PRESENT** | Required for voice trip draft speech streaming |
| `android.permission.CAMERA` | **NOT PRESENT** | Potential: Profile photo capture |

---

## 9. Navigation Architecture

- **Navigation Technology**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **Jetpack Navigation**: Not declared in dependencies.
- **Compose Navigation**: Not declared in dependencies.
- **Navigation Graph**: None.
- **Current Flow**:
  ```
  App Launch
      ↓
  MainActivity.onCreate()
      ↓
  IsharaTheme
      ↓
  Scaffold -> Greeting("Android")
  ```
- **Role-Based Navigation (Passenger vs Driver)**: Not implemented.

---

## 10. Existing Screens

| Screen Name | File | Technology | Role | Purpose | Navigation Entry | Backend Dependencies | State Management | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Greeting** | [MainActivity.kt:L33-L39](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt#L33-L39) | Jetpack Compose | None | Default template greeting | App launch (`MainActivity`) | None | Stateless | **Placeholder** |

**Total Screens Found**: 1 (Default Android Studio placeholder).  
**Application Screens (Auth, Home, Map, Trip, Driver Context, SOS, Settings)**: **NOT PRESENT IN CURRENT ANDROID PROJECT**.

---

## 11. State Management

- **ViewModel**: Not implemented (0 ViewModels in codebase).
- **StateFlow / SharedFlow**: Not used.
- **LiveData**: Not used.
- **Compose State (`remember`, `mutableStateOf`)**: Not used in production code.
- **SavedStateHandle**: Not used.
- **Current State Location**: No runtime state exists beyond the Activity lifecycle and local Composable parameters.
- **Architectural Risk**: High risk of anti-patterns if developers start putting mutable state directly into Composables without a structured MVI/MVVM ViewModel layer.

---

## 12. API / Networking

- **HTTP Client**: **NOT PRESENT IN CURRENT ANDROID PROJECT** (no Retrofit, OkHttp, Ktor, or HttpURLConnection code).
- **Base URL**: None configured in code.
- **Target Base URL**: `https://reposnse-ishaara.onrender.com` (with `/api/v1` prefix).
- **Interceptors / Auth Headers**: None.
- **Serializers**: None.
- **Data Models**: None.
- **Network State Handling**: None.
- **Secrets Found**: No hardcoded API keys or credentials detected in Android code.

---

## 13. Authentication

- **Authentication Implementation**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **Target Auth System**: Better Auth (`POST /api/auth/sign-in/social` with Google ID token, session cookies, and Bearer token).
- **Token Management**: No token storage, refresh interceptor, or session clearing logic.
- **Role Selection**: No role assignment onboarding (`POST /api/v1/users/me/onboarding` for `USER` vs `DRIVER_CONDUCTOR`).

---

## 14. Local Storage

- **DataStore**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **Room / SQLite**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **EncryptedSharedPreferences**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **SharedPreferences**: Not used.
- **Local Persistence Found**: None.

---

## 15. Location / Maps

- **FusedLocationProviderClient**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **LocationManager**: Not used.
- **Google Maps SDK / Mapbox**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **Background Location Service**: None.
- **Target Spatial Compatibility**: The backend coordinates format is strictly `[longitude, latitude]` (`[lng, lat]`). Standard Android `android.location.Location` and Google Maps LatLng use `(latitude, longitude)`. A bidirectional mapper is mandatory.

---

## 16. Firebase / Notifications

- **google-services.json**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **Google Services Gradle Plugin**: Not applied in `build.gradle.kts` or `plugins`.
- **Firebase Cloud Messaging (FCM)**: Not integrated.
- **Target Push Requirements**: Backend provides `POST /api/v1/devices/push-token` and `DELETE /api/v1/devices/push-token`.

---

## 17. Realtime / WebSockets

- **WebSocket Implementation**: **NOT PRESENT IN CURRENT ANDROID PROJECT**
- **Target Realtime Endpoints**:
  - `wss://<host>/api/v1/voice/realtime` (Driver voice streaming)
  - `wss://<host>/api/v1/discovery/realtime` (Passenger trip discovery stream)
  - `wss://<host>/api/v1/ride-requests/realtime` (Request status updates)
  - `wss://<host>/api/v1/rides/realtime` (Live GPS telemetry & tracking)

---

## 18. Assets

### Repository Assets (`assets/`)
1. **Brand Logos**:
   - `assets/images/ishara-primary-logo.png` (201 KB)
   - `assets/images/ishara-black-logo.png` (689 KB)
   - `assets/images/ishara-white-logo.png` (367 KB)
   - `assets/images/white-logo.png` (314 KB)
   - `assets/images/compact_logo.png` (521 KB)
   - `assets/images/horizontal_logo.png` (316 KB)
   - `assets/images/favicon_icon.png` (836 KB)
2. **Vehicle & App Illustrations**:
   - `assets/images/bus_sideview.png` (6.7 MB)
   - `assets/images/bus_backview.png` (6.2 MB)
   - `assets/images/bus_black&white.png` (1.6 MB)
   - `assets/images/ishara_bus.jpg` (238 KB)
   - `assets/images/android_app.png` (1.0 MB)
   - `assets/images/web_app.png` (856 KB)
3. **Motion Assets**:
   - `assets/logo-forming.mp4` (1.5 MB)
   - `assets/videos/front_view__Great_for_.mp4` (5.7 MB)
   - `assets/videos/Front_view__Useful_for___ve.mp4` (4.5 MB)
   - `assets/videos/Perfect_side_profile_view_of_t.mp4` (7.4 MB)
   - `assets/videos/Rear_view__Useful_for___tri.mp4` (6.8 MB)

### App Module Assets (`app/src/main/`)
- `app/src/main/assets/images/ishara_bus.jpg` (238 KB)
- `app/src/main/res/drawable/ishara_bus.jpg` (238 KB)
- `app/src/main/res/drawable/ic_launcher_background.xml` & `ic_launcher_foreground.xml`

**Branding Assessment**: The assets directory contains high-resolution, authentic Ishaara brand materials and bus visuals. However, large media files (e.g. 6-7MB uncompressed PNGs and 7MB MP4s) directly in the repo will need optimization or WebP conversion before inclusion into the final APK package.

---

## 19. Testing

- **Unit Tests**: Exactly 1 dummy test in [ExampleUnitTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/ExampleUnitTest.kt#L12-L17) (`assertEquals(4, 2 + 2)`).
- **Instrumented Tests**: Exactly 1 dummy test in [ExampleInstrumentedTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/androidTest/java/com/ishara/app/ExampleInstrumentedTest.kt#L16-L23) (`appContext.packageName == "com.ishara.app"`).
- **Compose UI Tests**: 0 implemented (dependencies declared, but no tests written).
- **Repository / ViewModel / API Tests**: 0.
- **Coverage**: **0%** domain code coverage.

---

## 20. Security Findings

| Severity | Finding | Location | Description |
| :--- | :--- | :--- | :--- |
| **MEDIUM** | Missing Network Security Config | `AndroidManifest.xml` | No explicit network security config. Cleartext HTTP is disabled by default on API 28+, but local development (`http://localhost:5000` or local LAN IP) will require explicit cleartext exception or SSL proxy. |
| **LOW** | R8 Optimization Disabled in Release | [app/build.gradle.kts:L25](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts#L25) | `release { optimization { enable = false } }` leaves code un-obfuscated and un-shrunk. |
| **LOW** | Auto-backup Enabled with Default Rules | [AndroidManifest.xml:L6-L8](file:///home/dev/Desktop/ishara-frontend/app/src/main/AndroidManifest.xml#L6-L8) | `allowBackup="true"`; sensitive session tokens could be backed up if storage rules are not configured. |
| **INFO** | Hardcoded Secrets Audit | All files | **No hardcoded secrets, API tokens, or keys detected** in repository source files. |

---

## 21. Code Quality Findings

- **Architecture Layering**: None present. The project is an untouched starter template.
- **Theme & Styling**: Default purple/pink Material3 palette in [Color.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/ui/theme/Color.kt) and [Theme.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/ui/theme/Theme.kt). Not aligned with Ishaara brand colors (primary orange/yellow/amber & dark transit themes seen in asset graphics).
- **Coupling & Memory Leaks**: None (codebase consists of template functions).

---

## 22. Production Readiness by Category

| Category | Status | Notes |
| :--- | :--- | :--- |
| **ARCHITECTURE** | **NOT IMPLEMENTED** | No clean architecture, MVI/MVVM, or multi-module / package layering. |
| **BUILD** | **PARTIAL** | AGP 9.4.1 + Gradle 9.6.0 builds, but release optimization is disabled. |
| **DEPENDENCIES** | **PARTIAL** | Only basic Compose & AndroidX dependencies declared in version catalog. |
| **UI** | **PARTIAL** | Pure Compose + Material3 established; zero real screens implemented. |
| **NAVIGATION** | **NOT IMPLEMENTED** | No Navigation library or routing structure. |
| **API / NETWORKING** | **NOT IMPLEMENTED** | No HTTP client, interceptors, or endpoints. |
| **AUTH** | **NOT IMPLEMENTED** | Better Auth / Google Social Sign-In not integrated. |
| **SECURITY** | **NEEDS REVIEW** | Optimization disabled; backup rules need session exclusions. |
| **STATE** | **NOT IMPLEMENTED** | No ViewModel or StateFlow architecture. |
| **LOCATION** | **NOT IMPLEMENTED** | No FusedLocationProviderClient or background service. |
| **REALTIME** | **NOT IMPLEMENTED** | No WebSocket infrastructure. |
| **STORAGE** | **NOT IMPLEMENTED** | No DataStore or Room database. |
| **NOTIFICATIONS** | **NOT IMPLEMENTED** | No FCM or notification channel management. |
| **TESTING** | **NOT IMPLEMENTED** | Only template tests exist (0% coverage). |
| **PERFORMANCE** | **NEEDS REVIEW** | Baseline Compose setup; heavy assets need bundling optimization. |
| **ACCESSIBILITY** | **UNKNOWN** | No custom UI exists to evaluate. |
| **RELEASE CONFIG** | **NOT IMPLEMENTED** | Unsigned, unoptimized release build type. |

---

## 23. Backend Alignment

The production backend specification has been codified into [docs/API_REFERENCE.md](file:///home/dev/Desktop/ishara-frontend/docs/API_REFERENCE.md).

### Alignment Comparison
| Backend Domain | Android Status | Compatibility Notes |
| :--- | :--- | :--- |
| **Base URL** (`https://reposnse-ishaara.onrender.com/api/v1`) | **NOT PRESENT** | Must configure environment-driven BuildConfig or Retrofit/Ktor base URL. |
| **Auth** (`/api/auth/*`) | **NOT PRESENT** | Requires Google Sign-In / Credential Manager & Bearer token header interceptor. |
| **Coordinate Order** (`[lng, lat]`) | **NOT PRESENT** | Critical: Backend expects `[longitude, latitude]`. Android Location provides `lat, lng`. Serialization adapter required. |
| **Users / Onboarding** (`/api/v1/users/me/*`) | **NOT PRESENT** | Required for role selection (`USER` vs `DRIVER_CONDUCTOR`). |
| **Driver Operations** (`/api/v1/drivers/*`) | **NOT PRESENT** | Includes GPS ping ingestion (`PATCH /api/v1/drivers/me/location`), online/offline toggle, and earnings. |
| **Trips & Discovery** (`/api/v1/trips/*`, `/api/v1/discovery/*`) | **NOT PRESENT** | Heuristic matching and trip creation endpoints. |
| **Ride Requests & Rides** (`/api/v1/ride-requests/*`, `/api/v1/rides/*`) | **NOT PRESENT** | Full ride lifecycle state machine (REQUESTED → CONFIRMED → DRIVER_ARRIVED → IN_PROGRESS → COMPLETED). |
| **Realtime WebSockets** (4 streaming channels) | **NOT PRESENT** | Live GPS tracking (`/api/v1/rides/realtime`), Ride requests (`/api/v1/ride-requests/realtime`), Discovery (`/api/v1/discovery/realtime`), Voice (`/api/v1/voice/realtime`). |
| **Safety / SOS** (`/api/v1/rides/:id/safety/sos`) | **NOT PRESENT** | Emergency trigger, active alert observer, emergency contacts. |
| **Payments** (`/api/v1/rides/:id/payment`) | **NOT PRESENT** | Razorpay SDK integration for UPI / Card checkout. |

---

## 24. Architecture Gaps

### Gap 1: Manifest Permissions & Component Registration
- **CURRENT**: Zero permissions in `AndroidManifest.xml`. Only `MainActivity`.
- **TARGET**: Full runtime permissions (`INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`) and Foreground Service declarations for driver tracking.
- **GAP**: App cannot make network calls or read device GPS until permissions are declared and granted.
- **IMPACT**: Complete blocker for app operation.
- **RECOMMENDATION**: Update `AndroidManifest.xml` with required permissions and foreground service declarations in Phase 01.

### Gap 2: Core Networking & Coordinate Serialization
- **CURRENT**: No HTTP client or serializers.
- **TARGET**: OkHttp + Retrofit (or Ktor) with authentication interceptor, logging interceptor, and a custom GeoJSON serializer converting between `[longitude, latitude]` and Android `Location` / domain `Coordinates(lat, lng)`.
- **GAP**: Complete networking layer missing.
- **IMPACT**: Cannot communicate with the backend.
- **RECOMMENDATION**: Implement Network module with typed API interfaces matching [docs/API_REFERENCE.md](file:///home/dev/Desktop/ishara-frontend/docs/API_REFERENCE.md).

### Gap 3: Role-Based Navigation & Dual Experience
- **CURRENT**: No navigation library. Single greeting composable.
- **TARGET**: Navigation Compose supporting splash, authentication, onboarding (role selection), User/Passenger Graph, and Driver/Conductor Graph.
- **GAP**: Entire navigation framework missing.
- **IMPACT**: Users cannot authenticate or navigate between screens.
- **RECOMMENDATION**: Integrate `androidx.navigation:navigation-compose` with type-safe route definitions.

### Gap 4: Session & Token Storage
- **CURRENT**: No local storage.
- **TARGET**: Encrypted DataStore or EncryptedSharedPreferences storing `sessionToken`, `userId`, `userRole`, and active `tripId`/`rideId`.
- **GAP**: Token persistence missing.
- **IMPACT**: Users will be logged out on every app recreation.
- **RECOMMENDATION**: Add `androidx.datastore:datastore-preferences` with AES encryption.

### Gap 5: Realtime & Background Location Service
- **CURRENT**: No WebSocket client, no location provider, no service.
- **TARGET**: Resilient OkHttp WebSocket manager with auto-reconnect and a Foreground Service using `FusedLocationProviderClient` for high-frequency driver GPS updates (`PATCH /api/v1/drivers/me/location`).
- **GAP**: Complete tracking and live updates infrastructure missing.
- **IMPACT**: Core mobility functionality (realtime bus tracking) is inoperable.
- **RECOMMENDATION**: Architect dedicated `LocationTracker` and `RealtimeChannelClient`.

---

## 25. Risks

1. **Coordinate Inversion Hazard**: Backend expects `[lng, lat]` (GeoJSON array format), while Android APIs and Google Maps use `(lat, lng)`. Without a strict domain value object and unit tests, coordinates will be inverted, causing buses to appear in oceans or invalid world locations.
2. **Foreground Service Battery Optimization & Android 14/15 Restrictions**: Background GPS tracking for drivers will be killed by OEM battery savers (Xiaomi, Samsung, etc.) unless a proper foreground service with `FOREGROUND_SERVICE_LOCATION` type and a sticky notification is implemented.
3. **Large Asset Packaging**: Over 30MB of high-resolution images and videos exist in `assets/`. Directly bundling these into the APK will result in excessive APK sizes. Video streaming or compressed WebP conversion will be necessary.
4. **WebSocket Reconnect Fragility**: Mobile network transitions (4G to 5G to WiFi) will sever WebSockets. A stateful connection manager with heartbeat ping/pong and exponential backoff is required.

---

## 26. Recommended Next Phase (Phase 01)

In **Phase 01 — Architecture & Core Infrastructure Setup**:
1. **Manifest & Permissions**: Declare all required permissions (`INTERNET`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`, etc.) in `AndroidManifest.xml`.
2. **Dependencies**: Add necessary production dependencies to `gradle/libs.versions.toml`:
   - Networking: Retrofit 2 / OkHttp 4 / kotlinx.serialization (or Moshi)
   - Navigation: `androidx.navigation:navigation-compose`
   - DI: Hilt or Koin
   - Storage: `androidx.datastore:datastore-preferences`
   - Location: `com.google.android.gms:play-services-location`
   - Realtime: OkHttp WebSocket
3. **Core Layer Setup**:
   - `core:network`: Base URL configuration, Auth Interceptor, Error Handler, GeoJSON coordinate serializer.
   - `core:datastore`: Secure session storage for tokens and user profile/role.
   - `core:designsystem`: Ishaara brand color tokens, typography, and reusable components based on existing brand assets.
4. **Navigation Framework**: Set up type-safe Compose Navigation graph separating Auth, User/Student, and Driver/Conductor flows.

---

## 27. Files Inspected

1. [settings.gradle.kts](file:///home/dev/Desktop/ishara-frontend/settings.gradle.kts) (lines 1–27)
2. [build.gradle.kts](file:///home/dev/Desktop/ishara-frontend/build.gradle.kts) (lines 1–5)
3. [gradle/libs.versions.toml](file:///home/dev/Desktop/ishara-frontend/gradle/libs.versions.toml) (lines 1–32)
4. [gradle/wrapper/gradle-wrapper.properties](file:///home/dev/Desktop/ishara-frontend/gradle/wrapper/gradle-wrapper.properties) (lines 1–10)
5. [gradle.properties](file:///home/dev/Desktop/ishara-frontend/gradle.properties) (lines 1–19)
6. [local.properties](file:///home/dev/Desktop/ishara-frontend/local.properties) (lines 1–10)
7. [.gitignore](file:///home/dev/Desktop/ishara-frontend/.gitignore) (lines 1–16)
8. [app/build.gradle.kts](file:///home/dev/Desktop/ishara-frontend/app/build.gradle.kts) (lines 1–54)
9. [app/.gitignore](file:///home/dev/Desktop/ishara-frontend/app/.gitignore) (line 1)
10. [app/src/main/AndroidManifest.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/AndroidManifest.xml) (lines 1–28)
11. [app/src/main/keepRules/rules.keep](file:///home/dev/Desktop/ishara-frontend/app/src/main/keepRules/rules.keep) (lines 1–12)
12. [app/src/main/java/com/ishara/app/MainActivity.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/MainActivity.kt) (lines 1–47)
13. [app/src/main/java/com/ishara/app/ui/theme/Color.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/ui/theme/Color.kt) (lines 1–11)
14. [app/src/main/java/com/ishara/app/ui/theme/Theme.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/ui/theme/Theme.kt) (lines 1–58)
15. [app/src/main/java/com/ishara/app/ui/theme/Type.kt](file:///home/dev/Desktop/ishara-frontend/app/src/main/java/com/ishara/app/ui/theme/Type.kt) (lines 1–34)
16. [app/src/main/res/values/strings.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/res/values/strings.xml) (lines 1–3)
17. [app/src/main/res/values/themes.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/res/values/themes.xml) (lines 1–5)
18. [app/src/main/res/values/colors.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/res/values/colors.xml) (lines 1–10)
19. [app/src/main/res/xml/backup_rules.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/res/xml/backup_rules.xml) (lines 1–13)
20. [app/src/main/res/xml/data_extraction_rules.xml](file:///home/dev/Desktop/ishara-frontend/app/src/main/res/xml/data_extraction_rules.xml) (lines 1–19)
21. [app/src/test/java/com/ishara/app/ExampleUnitTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/test/java/com/ishara/app/ExampleUnitTest.kt) (lines 1–17)
22. [app/src/androidTest/java/com/ishara/app/ExampleInstrumentedTest.kt](file:///home/dev/Desktop/ishara-frontend/app/src/androidTest/java/com/ishara/app/ExampleInstrumentedTest.kt) (lines 1–24)
23. Asset directories: `assets/images/`, `assets/videos/`, `app/src/main/assets/`, `app/src/main/res/drawable/`
