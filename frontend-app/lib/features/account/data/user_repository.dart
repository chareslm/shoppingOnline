import 'package:shopping_app/features/account/data/user_api.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';

class UserRepository {
  UserRepository(this._api);

  final UserApi _api;

  Future<UserProfile> profile() => _api.profile();

  Future<UserProfile> updateProfile({
    required String nickname,
    required String avatarUrl,
    required String realName,
    required String gender,
    required DateTime? birthday,
    required String bio,
  }) => _api.updateProfile(
    nickname: nickname,
    avatarUrl: avatarUrl,
    realName: realName,
    gender: gender,
    birthday: birthday,
    bio: bio,
  );

  Future<List<UserAddress>> addresses() => _api.addresses();
  Future<UserAddress> createAddress(SaveUserAddress address) =>
      _api.createAddress(address);
  Future<UserAddress> updateAddress(int id, SaveUserAddress address) =>
      _api.updateAddress(id, address);
  Future<void> setDefaultAddress(int id) => _api.setDefaultAddress(id);
  Future<void> deleteAddress(int id) => _api.deleteAddress(id);
  Future<UserPreference> preference() => _api.preference();
  Future<UserPreference> updatePreference({
    required bool marketingEnabled,
    required bool orderNotificationEnabled,
    required bool systemNotificationEnabled,
    required Map<String, Object?> extraPreferences,
  }) => _api.updatePreference(
    marketingEnabled: marketingEnabled,
    orderNotificationEnabled: orderNotificationEnabled,
    systemNotificationEnabled: systemNotificationEnabled,
    extraPreferences: extraPreferences,
  );
}
