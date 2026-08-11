import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/account/domain/auth_models.dart';

class AuthApi {
  AuthApi(this._client);

  final ApiClient _client;

  Future<RegisteredUser> register({
    required String username,
    required String password,
  }) async {
    final response = await _client.post(
      '/api/auth/register',
      data: {'username': username, 'password': password},
    );
    return unwrapApiResponse(response.data, RegisteredUser.fromJson);
  }

  Future<LoginResult> login({
    required String identifier,
    required String password,
    required String deviceId,
    required String deviceName,
  }) async {
    final response = await _client.post(
      '/api/auth/login/password',
      data: {
        'identifier': identifier,
        'password': password,
        'deviceId': deviceId,
        'deviceType': 'ANDROID',
        'deviceName': deviceName,
      },
    );
    return unwrapApiResponse(response.data, LoginResult.fromJson);
  }

  Future<void> logout(String deviceId) async {
    final response = await _client.post(
      '/api/auth/logout',
      data: {'deviceId': deviceId},
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    final response = await _client.put(
      '/api/auth/password',
      data: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<AuthenticatedUser> currentUser() async {
    final response = await _client.get('/api/auth/me');
    return unwrapApiResponse(response.data, AuthenticatedUser.fromJson);
  }
}
