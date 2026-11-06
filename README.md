# NetBlock

An Android app that cuts off internet access for the apps you pick. It does this with a local VPN that never leaves the device: the selected apps are routed into a tunnel and their packets are dropped. Every other app uses the normal network.

## Usage

1. Choose the apps to block with the switches in the list (search by name or package).
2. Turn on the master switch and accept Android's one-time VPN prompt.

Blocking is restored after a reboot if it was on before. The notification is the only sign of it running; remove it by turning the master switch off.

## Limitations

- Android allows one active VPN at a time. NetBlock can't run alongside another VPN app, and starting one will turn NetBlock off.
- It blocks per app, not per domain or per connection.
- Only apps with a launcher icon are listed.

## Build

Requires JDK 17+ and the Android SDK.

```
./gradlew assembleDebug
```

Kotlin 2.0.21, Jetpack Compose (Material 3), AGP 8.7.3, Gradle 8.9. Min SDK 26, target SDK 35.

## Releases

Prebuilt APKs are on the [Releases](https://github.com/nicolous260/NetBlock/releases) page.

## License

MIT, see [LICENSE](LICENSE).
