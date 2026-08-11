import 'package:flutter/foundation.dart';
import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/core/storage/session_store.dart';

enum AuthStatus { restoring, unauthenticated, authenticated }

class AuthSessionController extends ChangeNotifier {
  AuthSessionController(this._store);

  final SessionStore _store;
  AuthStatus _status = AuthStatus.restoring;
  AuthSession? _session;

  AuthStatus get status => _status;
  AuthSession? get session => _session;
  AuthenticatedUser? get user => _session?.user;
  String? get accessToken => _session?.accessToken;
  String? get refreshToken => _session?.refreshToken;
  bool get isAuthenticated => _status == AuthStatus.authenticated;

  Future<void> restoreLocal() async {
    try {
      _session = await _store.read();
    } catch (_) {
      _session = null;
    }
    _status = _session == null
        ? AuthStatus.unauthenticated
        : AuthStatus.authenticated;
    notifyListeners();
  }

  Future<void> establish(AuthSession session) async {
    await _store.write(session);
    _session = session;
    _status = AuthStatus.authenticated;
    notifyListeners();
  }

  Future<void> updateTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    final current = _session;
    if (current == null) return;
    await establish(
      current.copyWith(accessToken: accessToken, refreshToken: refreshToken),
    );
  }

  Future<void> updateUser(AuthenticatedUser user) async {
    final current = _session;
    if (current == null) return;
    await establish(current.copyWith(user: user));
  }

  Future<void> clear() async {
    _session = null;
    _status = AuthStatus.unauthenticated;
    try {
      await _store.clear();
    } finally {
      notifyListeners();
    }
  }
}
