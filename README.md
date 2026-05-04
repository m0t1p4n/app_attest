# app_attest

Flutter plugin for device/app integrity checks:

- **iOS**: Apple DeviceCheck **App Attest** (`DCAppAttestService`).
- **Android**: Google Play **Play Integrity API**.

The plugin returns native attestation/assertion data to Flutter. Your backend
must verify every attestation, assertion, and Play Integrity token. Do not trust
these values only on the client.

## Platform requirements

### iOS

- iOS 14.0+.
- A real signed device for App Attest validation. App Attest is not a reliable
  simulator-only flow.
- App Attest capability/entitlement configured for your app identifier in Apple
  Developer settings and Xcode.
- Server-side verification following Apple's App Attest documentation.

### Android

- Android minSdk 21+.
- Google Play Integrity configured in Play Console / Google Cloud.
- A valid numeric Google Cloud project number.
- Server-generated nonce/challenge and backend verification of the returned JWS.

## Usage

Add the package to your Flutter app, then import it:

```dart
import 'package:app_attest/app_attest.dart';
```

### iOS App Attest

```dart
final supported = await AppAttest.isSupported();
if (!supported) {
  // Fallback: the device/OS does not support App Attest.
  return;
}

// 1. Ask your backend for a one-time challenge.
final challenge = 'server-generated-challenge';

// 2. Generate a key once and store the keyId after your backend accepts it.
final keyId = await AppAttest.generateKey();

// 3. Send attestation.attestationObject and keyId to your backend.
final attestation = await AppAttest.attestKey(
  keyId: keyId,
  challenge: challenge,
);

// 4. Later, generate assertions for new server challenges.
final assertion = await AppAttest.generateAssertion(
  keyId: keyId,
  challenge: 'another-server-challenge',
);
```

Returned iOS data is base64 encoded:

- `AppAttestAttestation.keyId`
- `AppAttestAttestation.attestationObject`
- `AppAttestAssertion.keyId`
- `AppAttestAssertion.assertionObject`

The plugin SHA-256 hashes the challenge natively before calling Apple's
`attestKey` / `generateAssertion`, as required by `DCAppAttestService`.

### Android Play Integrity

```dart
final supported = await AppAttest.isSupported();
if (!supported) {
  // Fallback: Play Integrity manager could not be initialized.
  return;
}

final token = await AppAttest.requestPlayIntegrityToken(
  nonce: 'server-generated-nonce',
  cloudProjectNumber: 123456789012,
);

// Send token to your backend for Play Integrity JWS verification.
```

## API

- `AppAttest.isSupported()`
- `AppAttest.generateKey()` — iOS only
- `AppAttest.attestKey({required keyId, required challenge})` — iOS only
- `AppAttest.generateAssertion({required keyId, required challenge})` — iOS only
- `AppAttest.requestPlayIntegrityToken({required nonce, required cloudProjectNumber})` — Android only

Unsupported platform calls throw `PlatformException` with code
`UNSUPPORTED_PLATFORM`.

## Development checks

```sh
flutter pub get
flutter analyze
flutter test
```

Native App Attest / Play Integrity behavior must also be validated in a real app
on real configured devices because both services depend on signing, app identity,
entitlements, Play Console setup, and backend verification.
