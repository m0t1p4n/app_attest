import 'method_channel_app_attest.dart';

/// Platform abstraction for App Attest / Play Integrity calls.
abstract class AppAttestPlatform {
  /// Creates a platform interface instance.
  AppAttestPlatform();

  /// The active platform implementation.
  static AppAttestPlatform instance = MethodChannelAppAttest();

  /// Returns whether the native integrity service is available.
  Future<bool> isSupported() {
    throw UnimplementedError('isSupported() has not been implemented.');
  }

  /// Generates an iOS App Attest key and returns its key identifier.
  Future<String> generateKey() {
    throw UnimplementedError('generateKey() has not been implemented.');
  }

  /// Attests an iOS App Attest key.
  Future<Map<dynamic, dynamic>> attestKey({
    required String keyId,
    required String challenge,
  }) {
    throw UnimplementedError('attestKey() has not been implemented.');
  }

  /// Generates an iOS App Attest assertion.
  Future<Map<dynamic, dynamic>> generateAssertion({
    required String keyId,
    required String challenge,
  }) {
    throw UnimplementedError('generateAssertion() has not been implemented.');
  }

  /// Prepares the Android Standard Play Integrity token provider.
  Future<void> preparePlayIntegrityTokenProvider({
    required int cloudProjectNumber,
  }) {
    throw UnimplementedError(
      'preparePlayIntegrityTokenProvider() has not been implemented.',
    );
  }

  /// Clears the cached Android Standard Play Integrity token provider.
  Future<void> clearPreparedPlayIntegrityTokenProvider() {
    throw UnimplementedError(
      'clearPreparedPlayIntegrityTokenProvider() has not been implemented.',
    );
  }

  /// Requests an Android Standard Play Integrity token.
  Future<String> requestStandardPlayIntegrityToken({
    required String requestHash,
  }) {
    throw UnimplementedError(
      'requestStandardPlayIntegrityToken() has not been implemented.',
    );
  }

  /// Legacy Play Integrity compatibility wrapper.
  Future<String> requestPlayIntegrityToken({
    required String nonce,
    required int cloudProjectNumber,
  }) {
    throw UnimplementedError(
      'requestPlayIntegrityToken() has not been implemented.',
    );
  }
}
