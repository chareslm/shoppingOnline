import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/material.dart';
import 'package:shopping_app/app/app.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/app/router.dart';
import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/core/auth/session_controller.dart';
import 'package:shopping_app/core/storage/session_store.dart';

void main() {
  testWidgets('未登录启动后显示登录页', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sessionStoreProvider.overrideWithValue(_MemoryStore())],
        child: const ShoppingApp(),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('登录综合电商平台'), findsOneWidget);
    expect(find.text('没有账号？立即注册'), findsOneWidget);
  });

  testWidgets('没有 USER 角色的账号进入无权限页', (tester) async {
    final store = _MemoryStore()
      ..value = const AuthSession(
        accessToken: 'access',
        refreshToken: 'refresh',
        user: AuthenticatedUser(
          userId: 8,
          username: 'admin',
          roles: ['ADMIN'],
          permissions: [],
        ),
      );
    final controller = AuthSessionController(store);
    await controller.restoreLocal();
    final container = ProviderContainer(
      overrides: [authSessionProvider.overrideWith((ref) => controller)],
    );
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(
          routerConfig: container.read(appRouterProvider),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('当前账号不能访问用户端'), findsOneWidget);
    expect(find.textContaining('ADMIN'), findsOneWidget);
  });
}

class _MemoryStore implements SessionStore {
  AuthSession? value;

  @override
  Future<void> clear() async => value = null;

  @override
  Future<AuthSession?> read() async => value;

  @override
  Future<void> write(AuthSession session) async => value = session;
}
