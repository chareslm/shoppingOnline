import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';

class UserApi {
  UserApi(this._client);

  final ApiClient _client;

  Future<UserProfile> profile() async {
    final response = await _client.get('/api/users/me/profile');
    return unwrapApiResponse(response.data, UserProfile.fromJson);
  }

  Future<UserProfile> updateProfile({
    required String nickname,
    required String avatarUrl,
    required String realName,
    required String gender,
    required DateTime? birthday,
    required String bio,
  }) async {
    final response = await _client.put(
      '/api/users/me/profile',
      data: {
        'nickname': nickname,
        'avatarUrl': avatarUrl,
        'realName': realName,
        'gender': gender,
        'birthday': birthday == null ? null : _dateOnly(birthday),
        'bio': bio,
      },
    );
    return unwrapApiResponse(response.data, UserProfile.fromJson);
  }

  Future<List<UserAddress>> addresses() async {
    final response = await _client.get('/api/users/me/addresses');
    return unwrapApiResponse(response.data, (data) {
      if (data is! List) return const <UserAddress>[];
      return data.map(UserAddress.fromJson).toList(growable: false);
    });
  }

  Future<UserAddress> createAddress(SaveUserAddress address) async {
    final response = await _client.post(
      '/api/users/me/addresses',
      data: address.toJson(),
    );
    return unwrapApiResponse(response.data, UserAddress.fromJson);
  }

  Future<UserAddress> updateAddress(
    String addressId,
    SaveUserAddress address,
  ) async {
    final response = await _client.put(
      '/api/users/me/addresses/$addressId',
      data: address.toJson(),
    );
    return unwrapApiResponse(response.data, UserAddress.fromJson);
  }

  Future<void> setDefaultAddress(String addressId) async {
    final response = await _client.put(
      '/api/users/me/addresses/$addressId/default',
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> deleteAddress(String addressId) async {
    final response = await _client.delete('/api/users/me/addresses/$addressId');
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<UserPreference> preference() async {
    final response = await _client.get('/api/users/me/preferences');
    return unwrapApiResponse(response.data, UserPreference.fromJson);
  }

  Future<UserPreference> updatePreference({
    required bool marketingEnabled,
    required bool orderNotificationEnabled,
    required bool systemNotificationEnabled,
    required Map<String, Object?> extraPreferences,
  }) async {
    final response = await _client.put(
      '/api/users/me/preferences',
      data: {
        'marketingEnabled': marketingEnabled,
        'orderNotificationEnabled': orderNotificationEnabled,
        'systemNotificationEnabled': systemNotificationEnabled,
        'extraPreferences': extraPreferences,
      },
    );
    return unwrapApiResponse(response.data, UserPreference.fromJson);
  }
}

String _dateOnly(DateTime value) =>
    '${value.year.toString().padLeft(4, '0')}-'
    '${value.month.toString().padLeft(2, '0')}-'
    '${value.day.toString().padLeft(2, '0')}';
