# Ishaara Android — Mobility Platform

<p align="center">
  <img src="assets/images/ishara-primary-logo.png" alt="Ishaara Logo" width="180"/>
</p>

<p align="center">
  <strong>Next-Generation Campus & Urban Transit Mobility Platform</strong><br>
  Built natively for Android using Kotlin & Jetpack Compose.
</p>

---

## 🚌 Overview

**Ishaara** is a mobility platform designed to modernize public and campus transit. The Android client application provides dedicated experiences for two primary roles:

1. **User / Student**: Real-time trip discovery, bus arrival estimates, seat availability, live route tracking, digital ticketing/payments, and emergency SOS alerts.
2. **Driver / Conductor**: Hands-free voice trip creation, real-time GPS telemetry broadcast, passenger boarding management, seat inventory, and operational dashboards.

> **Note**: Fleet operator administration and billing settlements are managed through the separate Ishaara Web Dashboard.

---

## 🏗️ Architecture

The project is built on **Clean Architecture** with **Unidirectional Data Flow (UDF)**:

- **Presentation Layer**: Jetpack Compose + Material 3 + ViewModels + `StateFlow`
- **Domain Layer**: Pure Kotlin business models, repository contracts, and single-responsibility use cases
- **Data Layer**: Repositories, Remote Data Sources, Local Session Caching, and GeoJSON `[longitude, latitude]` serialization mappers
- **Infrastructure / Core**: Reusable networking, session storage, GPS location providers, WebSockets, and notification dispatchers
- **Dependency Injection**: Compile-time manual `AppContainer` pattern owned by `IshaaraApplication`

For comprehensive architectural specifications, refer to [ISHAARA_ANDROID_ARCHITECTURE.md](ISHAARA_ANDROID_ARCHITECTURE.md).

---

## 🛠️ Tech Stack & SDK

| Technology | Specification |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 (100% Kotlin) |
| **UI Toolkit** | Jetpack Compose + Material 3 |
| **Min SDK** | API 24 (Android 7.0 Nougat) |
| **Target SDK** | API 37 (Android 17) |
| **Compile SDK** | API 37 |
| **Build Tools** | Android Gradle Plugin (AGP) 9.4.1 & Gradle 9.6.0 |
| **Asynchronous Engine** | Kotlin Coroutines & Flow |
| **Testing** | JUnit 4, Espresso, AndroidX Test Runner |

---

## 📁 Repository Structure

```
ishara-frontend/
├── app/                                 # Android Application Module
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # Runtime permissions & application config
│       │   ├── java/com/ishara/app/
│       │   │   ├── IshaaraApplication.kt# App entry & AppContainer owner
│       │   │   ├── MainActivity.kt      # Edge-to-Edge Compose Activity
│       │   │   ├── core/                # Common, result, network, storage, location, realtime, DI
│       │   │   ├── domain/              # Pure business models, repository contracts & use cases
│       │   │   ├── data/                # DTOs, mappers, data sources & repository implementations
│       │   │   ├── feature/             # Auth & role-specific presentation layers
│       │   │   └── navigation/          # Destinations & NavigationManager
│       │   └── res/                     # Drawables, mipmaps, strings, themes
│       └── test/                        # Architecture & unit tests
├── assets/                              # Brand logos, vehicle renders & video media
├── docs/
│   └── API_REFERENCE.md                 # Complete Ishaara Backend API contract
├── gradle/                              # Version catalog (libs.versions.toml) & wrapper
├── ISHAARA_ANDROID_PHASE_00_AUDIT.md    # Phase 00 Audit Report
└── ISHAARA_ANDROID_ARCHITECTURE.md      # Phase 01 Architecture Specification
```

---

## 🌐 Backend & API

The application connects to the Ishaara Backend:
- **Base URL**: `https://reposnse-ishaara.onrender.com`
- **Prefix**: `/api/v1`
- **Spatial Standard**: Coordinates strictly follow GeoJSON `[longitude, latitude]` format.
- Complete API documentation is available in [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md).

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or JDK 21 configured
- Android SDK Platform 37

### Build & Run
```bash
# Clone the repository
git clone https://github.com/Veda361/ishara-android-frontend.git
cd ishara-android-frontend

# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug
```

---

## 📄 License
Proprietary — All rights reserved by Ishaara.
