# NetBlock

Android app that blocks internet for your apps and lets you allow internet per app.
Uses a local VPN (VpnService) that drops packets from blocked apps only.

## Build (Android Studio on PC)
1. File > Open, choose this folder (the one containing `settings.gradle.kts`).
2. Wait for Gradle sync (first time downloads Gradle 8.9 and libraries; needs internet).
3. Plug in a real phone (USB debugging on) and press Run.
   Android Studio's bundled JDK 17+ is used automatically.

## Use
Turn on "Block internet", accept Android's one-time VPN prompt, then switch on the apps that may still use the internet.

## Versions
Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21 (Compose compiler plugin), compileSdk/targetSdk 35, minSdk 26.
