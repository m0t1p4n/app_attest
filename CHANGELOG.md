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
