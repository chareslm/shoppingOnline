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

  AuthSession toSession() => AuthSession(
    accessToken: accessToken,
    refreshToken: refreshToken,
    user: user,
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
      userId: (json['userId'] as num).toInt(),
      username: json['username'] as String,
      status: json['status'] as String,
    );
  }

  final int userId;
  final String username;
  final String status;
}
