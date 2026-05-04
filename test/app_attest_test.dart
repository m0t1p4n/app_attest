import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:app_attest/app_attest.dart';
import 'package:app_attest/src/method_channel_app_attest.dart';

class FakeAppAttestPlatform extends AppAttestPlatform {
  int? preparedCloudProjectNumber;

  @override
  Future<bool> isSupported() async => true;

  @override
  Future<String> generateKey() async => 'key-id';

  @override
  Future<Map<dynamic, dynamic>> attestKey({
    required String keyId,
    required String challenge,
  }) async {
    return <String, dynamic>{
      'keyId': keyId,
      'attestationObject': 'attestation-base64',
    };
  }

  @override
  Future<Map<dynamic, dynamic>> generateAssertion({
    required String keyId,
    required String challenge,
  }) async {
    return <String, dynamic>{
      'keyId': keyId,
      'assertionObject': 'assertion-base64',
    };
  }

  @override
  Future<void> preparePlayIntegrityTokenProvider({
    required int cloudProjectNumber,
  }) async {
    preparedCloudProjectNumber = cloudProjectNumber;
  }

  @override
  Future<void> clearPreparedPlayIntegrityTokenProvider() async {
    preparedCloudProjectNumber = null;
  }

  @override
  Future<String> requestStandardPlayIntegrityToken({
    required String requestHash,
  }) async {
    return 'standard-play-integrity-token:$requestHash:$preparedCloudProjectNumber';
  }

  @override
  Future<String> requestPlayIntegrityToken({
    required String nonce,
    required int cloudProjectNumber,
  }) async {
    preparedCloudProjectNumber = cloudProjectNumber;
    return 'standard-play-integrity-token:$nonce:$cloudProjectNumber';
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  tearDown(() {
    AppAttestPlatform.instance = MethodChannelAppAttest();
  });

  test('public API forwards to platform implementation', () async {
    AppAttestPlatform.instance = FakeAppAttestPlatform();

    expect(await AppAttest.isSupported(), isTrue);
    expect(await AppAttest.generateKey(), 'key-id');

    final attestation = await AppAttest.attestKey(
      keyId: 'key-id',
      challenge: 'server-challenge',
    );
    expect(attestation.keyId, 'key-id');
    expect(attestation.attestationObject, 'attestation-base64');

    final assertion = await AppAttest.generateAssertion(
      keyId: 'key-id',
      challenge: 'server-challenge',
    );
    expect(assertion.keyId, 'key-id');
    expect(assertion.assertionObject, 'assertion-base64');

    await AppAttest.preparePlayIntegrityTokenProvider(cloudProjectNumber: 123);
    expect(
      await AppAttest.requestStandardPlayIntegrityToken(requestHash: 'hash'),
      'standard-play-integrity-token:hash:123',
    );
    expect(
      await AppAttest.requestPlayIntegrityToken(
        nonce: 'legacy-hash',
        cloudProjectNumber: 123,
      ),
      'standard-play-integrity-token:legacy-hash:123',
    );
  });

  test('public API validates empty arguments', () {
    AppAttestPlatform.instance = FakeAppAttestPlatform();

    expect(
      () => AppAttest.attestKey(keyId: '', challenge: 'challenge'),
      throwsArgumentError,
    );
    expect(
      () => AppAttest.generateAssertion(keyId: 'key-id', challenge: ' '),
      throwsArgumentError,
    );
    expect(
      () => AppAttest.preparePlayIntegrityTokenProvider(cloudProjectNumber: 0),
      throwsArgumentError,
    );
    expect(
      () => AppAttest.requestStandardPlayIntegrityToken(requestHash: ' '),
      throwsArgumentError,
    );
    expect(
      () => AppAttest.requestPlayIntegrityToken(
        nonce: 'nonce',
        cloudProjectNumber: 0,
      ),
      throwsArgumentError,
    );
  });

  test('method channel maps native calls and arguments', () async {
    final implementation = MethodChannelAppAttest();
    final calls = <MethodCall>[];

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(implementation.methodChannel, (call) async {
          calls.add(call);
          switch (call.method) {
            case 'isSupported':
              return true;
            case 'generateKey':
              return 'key-id';
            case 'attestKey':
              return <String, dynamic>{
                'keyId': (call.arguments as Map<dynamic, dynamic>)['keyId'],
                'attestationObject': 'attestation-base64',
              };
            case 'generateAssertion':
              return <String, dynamic>{
                'keyId': (call.arguments as Map<dynamic, dynamic>)['keyId'],
                'assertionObject': 'assertion-base64',
              };
            case 'preparePlayIntegrityTokenProvider':
              return null;
            case 'clearPreparedPlayIntegrityTokenProvider':
              return null;
            case 'requestStandardPlayIntegrityToken':
              return 'standard-play-integrity-token';
          }
          return null;
        });

    expect(await implementation.isSupported(), isTrue);
    expect(await implementation.generateKey(), 'key-id');
    expect(
      await implementation.attestKey(keyId: 'key-id', challenge: 'challenge'),
      containsPair('attestationObject', 'attestation-base64'),
    );
    expect(
      await implementation.generateAssertion(
        keyId: 'key-id',
        challenge: 'challenge',
      ),
      containsPair('assertionObject', 'assertion-base64'),
    );
    await implementation.preparePlayIntegrityTokenProvider(
      cloudProjectNumber: 123,
    );
    expect(
      await implementation.requestStandardPlayIntegrityToken(
        requestHash: 'request-hash',
      ),
      'standard-play-integrity-token',
    );
    expect(
      await implementation.requestPlayIntegrityToken(
        nonce: 'legacy-hash',
        cloudProjectNumber: 456,
      ),
      'standard-play-integrity-token',
    );
    await implementation.clearPreparedPlayIntegrityTokenProvider();

    expect(calls.map((call) => call.method), <String>[
      'isSupported',
      'generateKey',
      'attestKey',
      'generateAssertion',
      'preparePlayIntegrityTokenProvider',
      'requestStandardPlayIntegrityToken',
      'preparePlayIntegrityTokenProvider',
      'requestStandardPlayIntegrityToken',
      'clearPreparedPlayIntegrityTokenProvider',
    ]);
    expect(calls[4].arguments, <String, dynamic>{'cloudProjectNumber': 123});
    expect(calls[6].arguments, <String, dynamic>{'cloudProjectNumber': 456});
    expect(calls[7].arguments, <String, dynamic>{'requestHash': 'legacy-hash'});

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(implementation.methodChannel, null);
  });
}
