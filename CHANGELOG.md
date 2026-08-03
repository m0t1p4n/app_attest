## 0.2.0

* Migrated the Android build to Built-in Kotlin: the plugin no longer applies the Kotlin Gradle Plugin (`kotlin-android`), so apps stop emitting the Flutter KGP deprecation warning and keep building on AGP 9+.
* Replaced the `kotlinOptions` block with the `kotlin { compilerOptions { ... } }` DSL.
* Bumped the Android toolchain to AGP 8.11.1 / Kotlin 2.2.20 and `compileSdk` 36, the versions required by Flutter 3.44.
* Raised the Java/Kotlin target from 8 to 17, as required by Flutter 3.44.
* Added `android/settings.gradle` so `android/` can be opened as a standalone Gradle project in Android Studio / IntelliJ.
* **Breaking:** requires Flutter >= 3.44.0 and Dart >= 3.12.0.

## 0.1.1

* Added Swift Package Manager support for iOS.
* Moved the iOS sources from `ios/Classes/` to `ios/app_attest/Sources/app_attest/`.
* Updated the podspec to build from the Swift package layout, so CocoaPods keeps working unchanged.

## 0.1.0

* Initial Flutter plugin implementation.
* Added iOS App Attest support for `isSupported`, `generateKey`, `attestKey`, and `generateAssertion`.
* Added Android Play Integrity Standard API support for provider warm-up and `requestHash` token requests.
* Added `preparePlayIntegrityTokenProvider`, `requestStandardPlayIntegrityToken`, and `clearPreparedPlayIntegrityTokenProvider`.
* Added separate Android Play Integrity Classic API support with `requestClassicPlayIntegrityToken`.
* Kept `requestPlayIntegrityToken` as a deprecated compatibility alias for Classic API requests.
* Reorganized Android native code into separate Standard and Classic Play Integrity clients.
* Added Dart API models and MethodChannel tests.
* Expanded README documentation for pub.dev publishing.
