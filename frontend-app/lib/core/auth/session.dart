import 'package:shopping_app/core/network/api_response.dart';

enum PortalMode { user, merchant }

const merchantPortalRoles = [
  'MERCHANT_OWNER',
  'MERCHANT_STAFF',
  'CUSTOMER_SERVICE',
];

bool hasMerchantPortalRole(List<String> roles) =>
    roles.any(merchantPortalRoles.contains);

bool isCustomerServiceOnly(List<String> roles) =>
    roles.contains('CUSTOMER_SERVICE') &&
    !roles.contains('MERCHANT_OWNER') &&
    !roles.contains('MERCHANT_STAFF');

bool allowsPortal(PortalMode mode, List<String> roles) => mode == PortalMode.user
    ? roles.contains('USER')
    : hasMerchantPortalRole(roles);

String portalHomePath(PortalMode mode, List<String> roles) {
  if (mode == PortalMode.merchant) {
    return isCustomerServiceOnly(roles) ? '/merchant/inbox' : '/merchant';
  }
  return '/';
}

PortalMode parsePortalMode(Object? value) =>
    value == 'merchant' ? PortalMode.merchant : PortalMode.user;

class AuthenticatedUser {
  const AuthenticatedUser({
    required this.userId,
    required this.username,
    required this.roles,
    required this.permissions,
    this.mustChangePassword = false,
  });

  factory AuthenticatedUser.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return AuthenticatedUser(
      userId: json['userId'].toString(),
      username: json['username'] as String,
      roles: readStringList(json['roles']),
      permissions: readStringList(json['permissions']),
      mustChangePassword: json['mustChangePassword'] == true,
    );
  }

  final String userId;
  final String username;
  final List<String> roles;
  final List<String> permissions;
  final bool mustChangePassword;

  Map<String, Object?> toJson() => {
    'userId': userId,
    'username': username,
    'roles': roles,
    'permissions': permissions,
    'mustChangePassword': mustChangePassword,
  };
}

class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
    this.portalMode = PortalMode.user,
  });

  factory AuthSession.fromJson(Object? value) {
    final json = requireJsonMap(value, message: '本地登录会话格式错误');
    return AuthSession(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      user: AuthenticatedUser.fromJson(json['user']),
      portalMode: parsePortalMode(json['portalMode']),
    );
  }

  final String accessToken;
  final String refreshToken;
  final AuthenticatedUser user;
  final PortalMode portalMode;

  AuthSession copyWith({
    String? accessToken,
    String? refreshToken,
    AuthenticatedUser? user,
    PortalMode? portalMode,
  }) => AuthSession(
    accessToken: accessToken ?? this.accessToken,
    refreshToken: refreshToken ?? this.refreshToken,
    user: user ?? this.user,
    portalMode: portalMode ?? this.portalMode,
  );

  Map<String, Object?> toJson() => {
    'accessToken': accessToken,
    'refreshToken': refreshToken,
    'user': user.toJson(),
    'portalMode': portalMode.name,
  };
}
