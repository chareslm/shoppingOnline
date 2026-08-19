import 'package:shopping_app/core/network/api_response.dart';

class AuthenticatedUser {
  const AuthenticatedUser({
    required this.userId,
    required this.username,
    required this.roles,
    required this.permissions,
  });

  factory AuthenticatedUser.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return AuthenticatedUser(
      userId: json['userId'].toString(),
      username: json['username'] as String,
      roles: readStringList(json['roles']),
      permissions: readStringList(json['permissions']),
    );
  }

  final String userId;
  final String username;
  final List<String> roles;
  final List<String> permissions;

  Map<String, Object?> toJson() => {
    'userId': userId,
    'username': username,
    'roles': roles,
    'permissions': permissions,
  };
}

class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
  });

  factory AuthSession.fromJson(Object? value) {
    final json = requireJsonMap(value, message: '本地登录会话格式错误');
    return AuthSession(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      user: AuthenticatedUser.fromJson(json['user']),
    );
  }

  final String accessToken;
  final String refreshToken;
  final AuthenticatedUser user;

  AuthSession copyWith({
    String? accessToken,
    String? refreshToken,
    AuthenticatedUser? user,
  }) => AuthSession(
    accessToken: accessToken ?? this.accessToken,
    refreshToken: refreshToken ?? this.refreshToken,
    user: user ?? this.user,
  );

  Map<String, Object?> toJson() => {
    'accessToken': accessToken,
    'refreshToken': refreshToken,
    'user': user.toJson(),
  };
}
