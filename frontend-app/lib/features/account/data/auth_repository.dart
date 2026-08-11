import 'package:shopping_app/core/auth/session_controller.dart';
import 'package:shopping_app/core/device/device_identity.dart';
import 'package:shopping_app/core/network/api_exception.dart';
import 'package:shopping_app/features/account/data/auth_api.dart';
import 'package:shopping_app/features/account/domain/auth_models.dart';

class AuthRepository {
  AuthRepository(this._api, this._sessionController, this._deviceIdentity);

  final AuthApi _api;
  final AuthSessionController _sessionController;
  final DeviceIdentity _deviceIdentity;

  Future<void> restoreSession() async {
    await _sessionController.restoreLocal();
    if (!_sessionController.isAuthenticated) return;

    try {
      final user = await _api.currentUser();
      await _sessionController.updateUser(user);
    } on ApiException catch (error) {
      if (error.isUnauthorized) await _sessionController.clear();
    }
  }

  Future<RegisteredUser> register({
    required String username,
    required String password,
  }) => _api.register(username: username, password: password);

  Future<void> login({
    required String identifier,
    required String password,
  }) async {
    final deviceId = await _deviceIdentity.getDeviceId();
    final deviceName = await _deviceIdentity.getDeviceName();
    final result = await _api.login(
      identifier: identifier,
      password: password,
      deviceId: deviceId,
      deviceName: deviceName,
    );
    await _sessionController.establish(result.toSession());
  }

  Future<void> logout() async {
    try {
      final deviceId = await _deviceIdentity.getDeviceId();
      if (_sessionController.isAuthenticated) await _api.logout(deviceId);
    } finally {
      await _sessionController.clear();
    }
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    await _api.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
    await _sessionController.clear();
  }
}
