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

  /// Requests an Android Play Integrity token.
  Future<String> requestPlayIntegrityToken({
    required String nonce,
    required int cloudProjectNumber,
  }) {
    throw UnimplementedError(
      'requestPlayIntegrityToken() has not been implemented.',
    );
  }
}
