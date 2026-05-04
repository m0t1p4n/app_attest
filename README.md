# app_attest

Flutter plugin for:

- **iOS** Apple App Attest (`DeviceCheck` / `DCAppAttestService`)
- **Android** Google Play Integrity **Standard API**

## What is implemented

### iOS

- `AppAttest.isSupported()`
- `AppAttest.generateKey()`
- `AppAttest.attestKey(...)`
- `AppAttest.generateAssertion(...)`

### Android

- `AppAttest.isSupported()`
- `AppAttest.preparePlayIntegrityTokenProvider(...)`
- `AppAttest.requestStandardPlayIntegrityToken(...)`
- `AppAttest.clearPreparedPlayIntegrityTokenProvider()`

The Android implementation follows the **Standard API** flow recommended by
Google Play:

1. warm up a token provider once
2. request tokens on demand with a `requestHash`

## Android Standard Play Integrity flow

Before requesting a token, prepare the provider once:

```dart
await AppAttest.preparePlayIntegrityTokenProvider(
  cloudProjectNumber: 123456789012,
);
```

When you need to protect a backend request, compute a stable request hash in
your app and request a token:

```dart
final token = await AppAttest.requestStandardPlayIntegrityToken(
  requestHash: 'sha256-of-stable-request-payload',
);
```

Send the returned token to your backend for decode + verification using Google
Play Integrity server APIs.

### Important

- `requestHash` should be a digest of the request you want to protect.
- Do **not** send sensitive plain text as `requestHash`.
- If the provider expires, prepare it again.
- Tokens must be verified on your backend, not trusted only on device.

### Optional reset

```dart
await AppAttest.clearPreparedPlayIntegrityTokenProvider();
```

## iOS App Attest flow

```dart
final supported = await AppAttest.isSupported();
if (!supported) return;

final keyId = await AppAttest.generateKey();

final attestation = await AppAttest.attestKey(
  keyId: keyId,
  challenge: 'server-generated-challenge',
);

final assertion = await AppAttest.generateAssertion(
  keyId: keyId,
  challenge: 'server-generated-challenge',
);
```

## Legacy Android method

`AppAttest.requestPlayIntegrityToken(...)` is kept as a deprecated compatibility
wrapper. Internally it now prepares the Standard API provider and uses the
provided `nonce` value as the Standard API `requestHash`.

Prefer the explicit Standard API methods for new code.
