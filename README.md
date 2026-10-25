# ⚡ Zenith90

> **Apex Readiness & 3-Pillar Daily Equilibrium Architecture**  
> A native Android habit discipline and transformation trajectory application engineered with **Jetpack Compose**, **Material 3**, **Room Persistence**, and **Firebase Authentication**.

---

## 📱 Download Test APK

You can download the pre-compiled APK directly from the GitHub Releases page:

👉 **[Download Zenith90 Debug APK (Releases)](https://github.com/suvankar-9510/Zenith90/releases/tag/v1.0.0-preview)**

1. Download `zenith90-debug.apk` directly on your Android device (Android 9.0+ / API 28+).
2. Tap the downloaded file to install (allow "Install Unknown Apps" in device settings if prompted).
3. Test using **Email/Password**, **Google Sign-In**, or **Continue as Guest (Offline Vault)**!

---

## 🏛 Core Pillars & Features

* **Pillar I • Circadian Mastery**: Target 05:30 AM circadian wake window with deviation telemetry.
* **Pillar II • Deep Study Engine**: 6-hour daily focus target with active Pomodoro/deep-work timer and subject tagging.
* **Pillar III • Physical Conditioning**: Gym Hypertrophy vs. Home Calisthenics toggle and completion tracking.
* **Night-Before Blueprint Architect**: Time-blocked objective scheduler and rollover system.
* **Consistency & Trajectory Matrix**: 120-day heatmap grid and cubic Bézier gradient curves tracking study velocity.
* **Milestone & Badge Vault**: Automated progression unlocks (The 5 AM Club, Iron & Ink, Relentless 30/60/90).
* **Identity & Data Segregation**: Firebase Cloud Auth + offline room persistence partitioned per `userId`.

---

## 🛠 Tech Stack

* **UI**: 100% Jetpack Compose BOM 2024.09.00, Material 3, Canvas Drawing
* **Architecture**: Unidirectional Data Flow (UDF), StateFlow, Coroutines, MVVM
* **Persistence**: AndroidX Room 2.6.1 with TypeConverters & Relations
* **Authentication**: Firebase Authentication KTX, Android Credential Manager, Google ID
* **Target SDK**: Android 15 (compileSdk 35, minSdk 28)
* **CI/CD**: GitHub Actions automated APK compilation and release publishing

---

## 🔨 Local Build

```bash
# Clone the repository
git clone https://github.com/suvankar-9510/Zenith90.git
cd Zenith90

# Build debug APK
./gradlew assembleDebug

# Install onto connected Android device or emulator
./gradlew installDebug
```
