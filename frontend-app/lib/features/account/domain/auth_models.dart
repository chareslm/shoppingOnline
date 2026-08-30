import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/core/network/api_response.dart';

class LoginResult {
  const LoginResult({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
  });

  factory LoginResult.fromJson(Object? value) {
    final json = requireJsonMap(value);
    final user = AuthenticatedUser.fromJson(json);
    return LoginResult(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      user: user,
    );
  }

  final String accessToken;
  final String refreshToken;
  final AuthenticatedUser user;

  AuthSession toSession(PortalMode portalMode) => AuthSession(
    accessToken: accessToken,
    refreshToken: refreshToken,
    user: user,
    portalMode: portalMode,
  );
}

class RegisteredUser {
  const RegisteredUser({
    required this.userId,
    required this.username,
    required this.status,
  });

  factory RegisteredUser.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return RegisteredUser(
      userId: json['userId'].toString(),
      username: json['username'] as String,
      status: json['status'] as String,
    );
  }

  final String userId;
  final String username;
  final String status;
}

class DeviceSession {
  const DeviceSession({
    required this.id,
    required this.deviceType,
    required this.status,
    required this.current,
    required this.lastActiveAt,
    required this.createdAt,
    this.deviceName,
    this.appVersion,
    this.maskedIp,
    this.sessionExpiresAt,
  });

  factory DeviceSession.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return DeviceSession(
      id: json['id'].toString(),
      deviceType: json['deviceType']?.toString() ?? 'UNKNOWN',
      deviceName: json['deviceName'] as String?,
      appVersion: json['appVersion'] as String?,
      maskedIp: json['maskedIp'] as String?,
      lastActiveAt: DateTime.parse(json['lastActiveAt'].toString()),
      createdAt: DateTime.parse(json['createdAt'].toString()),
      status: json['status']?.toString() ?? 'REVOKED',
      current: json['current'] == true,
      sessionExpiresAt: json['sessionExpiresAt'] == null
          ? null
          : DateTime.parse(json['sessionExpiresAt'].toString()),
    );
  }

  final String id;
  final String deviceType;
  final String? deviceName;
  final String? appVersion;
  final String? maskedIp;
  final DateTime lastActiveAt;
  final DateTime createdAt;
  final String status;
  final bool current;
  final DateTime? sessionExpiresAt;
}
