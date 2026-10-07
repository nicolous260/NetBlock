# 🛡️ NetBlock

**NetBlock** is a modern, open-source Android application that gives you granular control over your device's internet access by blocking unwanted internet traffic for selected apps using a local on-device VPN firewall.

---

## ✨ Features

- **Per-App Internet Blocking (Blocklist Mode)**: Easily select which installed applications should have their internet access blocked while allowing all other apps to connect normally.
- **Local VPN Firewall (`VpnService`)**: Routes only selected apps into a local black-hole VPN tunnel where packets are safely discarded—ensuring zero data leakage and full privacy.
- **Material 3 UI & Theming**: Designed with Google's latest Material 3 guidelines, featuring smooth cards, responsive layouts, and automatic **Light and Dark mode** support.
- **Instant App Search**: Instantly search and filter through all installed apps by name or package name.
- **Modern Tech Stack**: Built with Jetpack Compose, Kotlin Coroutines, and Android 14+ Foreground Service compliance (`specialUse`).

---

## 🚀 How It Works

1. **Turn On Protection**: Toggle the master switch to activate the local NetBlock VPN service.
2. **Accept Prompt**: Accept Android's one-time system VPN connection request dialog.
3. **Select Apps to Block**: Flip the switch next to any app in your installed list to instantly block its internet access.

---

## 🛠️ Tech Stack & Requirements

- **Min SDK**: 26 (Android 8.0+)
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Build System**: Gradle 8.9 / AGP 8.7.3

---

## 📥 Download & Releases

You can download the latest signed APK directly from the [Releases](https://github.com/nicolous260/NetBlock/releases) page.

---

## 📄 License

This project is open-source and available under the MIT License.
