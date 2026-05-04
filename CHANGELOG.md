## 0.0.1

* Initial Flutter plugin implementation.
* Added iOS App Attest support for `isSupported`, `generateKey`, `attestKey`, and `generateAssertion`.
* Added Android Play Integrity Standard API support for provider warm-up and `requestHash` token requests.
* Added `preparePlayIntegrityTokenProvider`, `requestStandardPlayIntegrityToken`, and `clearPreparedPlayIntegrityTokenProvider`.
* Kept `requestPlayIntegrityToken` as a deprecated compatibility wrapper.
* Added Dart API models and MethodChannel tests.
