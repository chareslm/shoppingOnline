import 'package:flutter_test/flutter_test.dart';
import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/core/auth/session_controller.dart';
import 'package:shopping_app/core/storage/session_store.dart';

void main() {
  const user = AuthenticatedUser(
    userId: 7,
    username: 'alice_1',
    roles: ['USER'],
    permissions: [],
  );
  const session = AuthSession(
    accessToken: 'access-1',
    refreshToken: 'refresh-1',
    user: user,
  );

  test('从安全存储恢复会话', () async {
    final controller = AuthSessionController(_MemoryStore(session));

    await controller.restoreLocal();

    expect(controller.status, AuthStatus.authenticated);
    expect(controller.user?.username, 'alice_1');
  });

  test('刷新后同时替换两个 Token', () async {
    final store = _MemoryStore(session);
    final controller = AuthSessionController(store);
    await controller.restoreLocal();

    await controller.updateTokens(
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
    );

    expect(controller.accessToken, 'access-2');
    expect(controller.refreshToken, 'refresh-2');
    expect(store.value?.accessToken, 'access-2');
  });

  test('清除会话后进入未登录状态', () async {
    final store = _MemoryStore(session);
    final controller = AuthSessionController(store);
    await controller.restoreLocal();

    await controller.clear();

    expect(controller.status, AuthStatus.unauthenticated);
    expect(store.value, isNull);
  });
}

class _MemoryStore implements SessionStore {
  _MemoryStore(this.value);

  AuthSession? value;

  @override
  Future<void> clear() async => value = null;

  @override
  Future<AuthSession?> read() async => value;

  @override
  Future<void> write(AuthSession session) async => value = session;
}
