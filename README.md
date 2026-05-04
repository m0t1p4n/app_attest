# app_attest

Flutter plugin for app/device integrity checks:

- **iOS**: Apple App Attest (`DeviceCheck` / `DCAppAttestService`)
- **Android**: Google Play Integrity **Standard API** and **Classic API**

The plugin only creates native attestation material on the device. Your backend
must verify all attestation objects, assertions, and Play Integrity tokens before
trusting the result.

## Features

| Platform | API | Methods |
| --- | --- | --- |
| iOS 14+ | Apple App Attest | `isSupported`, `generateKey`, `attestKey`, `generateAssertion` |
| Android API 21+ | Play Integrity Standard API | `preparePlayIntegrityTokenProvider`, `requestStandardPlayIntegrityToken`, `clearPreparedPlayIntegrityTokenProvider` |
| Android API 21+ | Play Integrity Classic API | `requestClassicPlayIntegrityToken` |

## Installation

Add the package to your Flutter app:

```yaml
dependencies:
  app_attest: ^0.1.0
```

Then run:

```sh
flutter pub get
```

## Platform setup

### iOS

App Attest requires:

- iOS 14.0 or later
- a real device for production validation
- an Apple Developer account
- App Attest capability/entitlement configured for your app identifier

In Xcode, enable the **App Attest** capability for your app target. App Attest
does not provide useful production guarantees from a simulator.

### Android

Play Integrity requires:

- Android 5.0/API 21 or later
- Google Play services and Play Store support on the device
- an app configured in Google Play Console
- a Google Cloud project number linked to Play Integrity

For server-side token decoding, configure a service account with the Play
Integrity API scope in the Google Cloud project associated with your app.

## iOS App Attest usage

### 1. Check support

```dart
final supported = await AppAttest.isSupported();
if (!supported) {
  // Fall back to another risk signal or block the protected action.
  return;
}
```

### 2. Generate and attest a key

Generate a key once and send the attestation object to your backend. Store the
`keyId` after your backend verifies the attestation.

```dart
final keyId = await AppAttest.generateKey();

final attestation = await AppAttest.attestKey(
  keyId: keyId,
  challenge: 'server-generated-one-time-challenge',
);

await sendAttestationToBackend(
  keyId: attestation.keyId,
  attestationObject: attestation.attestationObject,
);
```

The plugin hashes `challenge` with SHA-256 natively before calling Apple's
`DCAppAttestService.attestKey`.

### 3. Generate assertions for protected requests

After the key has been attested and accepted by your backend, generate an
assertion when making a protected server request.

```dart
final assertion = await AppAttest.generateAssertion(
  keyId: storedKeyId,
  challenge: 'server-generated-one-time-challenge-or-request-hash',
);

await sendAssertionToBackend(
  keyId: assertion.keyId,
  assertionObject: assertion.assertionObject,
);
```

Your backend must verify the App Attest certificate chain, challenge, app/team
identifier, sign count, and assertion signature according to Apple's App Attest
documentation.

## Android Play Integrity Standard API usage

The Standard API is the recommended Play Integrity flow for new integrations. It
has two separate steps:

1. **Prepare** a token provider in advance.
2. **Request** a token on demand with a `requestHash` for the protected action.

### 1. Prepare the token provider

Call this on app launch, warm start, sign-in start, or shortly before a protected
backend request.

```dart
await AppAttest.preparePlayIntegrityTokenProvider(
  cloudProjectNumber: 123456789012,
);
```

Google recommends preparing the provider before the critical path because warm-up
can take a few seconds. Each app instance can prepare up to a limited number of
times per minute, so avoid calling it before every request.

### 2. Request a token with a request hash

Compute a stable digest of the request data you want to protect, then request the
token:

```dart
final token = await AppAttest.requestStandardPlayIntegrityToken(
  requestHash: 'sha256-of-stable-request-payload',
);

await sendPlayIntegrityTokenToBackend(token);
```

Important rules for `requestHash`:

- Include the important request parameters or user action data.
- Use a stable serialization before hashing.
- Do not put sensitive plaintext into `requestHash`; hash it first.
- Keep it within Google Play Integrity limits.

If the provider expires, Android may return
`INTEGRITY_TOKEN_PROVIDER_INVALID`. Prepare a new provider and retry the user
action when appropriate.

### Optional: clear the prepared provider

```dart
await AppAttest.clearPreparedPlayIntegrityTokenProvider();
```

This is useful when the user signs out, changes environment, or you want to force
a fresh warm-up.

## Android Play Integrity Classic API usage

Classic API requests are nonce-based and do not use the Standard API provider
warm-up. Prefer the Standard API for new apps, but use Classic API when your
backend still expects the original nonce-based token flow.

```dart
final token = await AppAttest.requestClassicPlayIntegrityToken(
  nonce: 'base64-or-url-safe-request-nonce',
  cloudProjectNumber: 123456789012,
);

await sendPlayIntegrityTokenToBackend(token);
```

`requestPlayIntegrityToken` is kept as a deprecated compatibility alias for
`requestClassicPlayIntegrityToken`.

## Backend verification

Never trust client-side success alone.

### iOS

Verify App Attest data on your backend using Apple's App Attest requirements:

- challenge/client data hash
- Apple certificate chain and App Attest environment
- app identifier/team identifier
- key identifier
- sign count and assertion signature

### Android

Send the encrypted Play Integrity token to your backend and decode it with
Google's Play Integrity API:

```text
POST https://playintegrity.googleapis.com/v1/PACKAGE_NAME:decodeIntegrityToken
```

After decoding, validate at least:

- package name
- certificate digest
- app recognition verdict
- device integrity verdict
- licensing verdict, if relevant
- `requestHash` for Standard API requests
- nonce for Classic API requests

## Error handling

Native failures are returned as `PlatformException`s. Android Play Integrity
errors are mapped to readable codes such as:

- `API_NOT_AVAILABLE`
- `PLAY_STORE_NOT_FOUND`
- `NETWORK_ERROR`
- `TOO_MANY_REQUESTS`
- `CLOUD_PROJECT_NUMBER_IS_INVALID`
- `INTEGRITY_TOKEN_PROVIDER_INVALID`
- `REQUEST_HASH_TOO_LONG`

iOS returns `UNSUPPORTED_PLATFORM` when App Attest is not available on the device
or OS version.

## Publishing notes

Before publishing a new version, run:

```sh
dart format lib test
flutter analyze
flutter test
flutter pub publish --dry-run
```

## License

MIT
