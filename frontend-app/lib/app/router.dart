import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/app/module_registry.dart';
import 'package:shopping_app/core/auth/session_controller.dart';
import 'package:shopping_app/features/account/presentation/forbidden_page.dart';
import 'package:shopping_app/features/account/presentation/login_page.dart';
import 'package:shopping_app/features/account/presentation/register_page.dart';
import 'package:shopping_app/features/account/presentation/splash_page.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final session = ref.read(authSessionProvider);
  final router = GoRouter(
    initialLocation: '/splash',
    refreshListenable: session,
    redirect: (context, state) {
      final location = state.matchedLocation;
      final isAuthPage = location == '/login' || location == '/register';

      if (session.status == AuthStatus.restoring) {
        return location == '/splash' ? null : '/splash';
      }
      if (!session.isAuthenticated) {
        if (isAuthPage) return null;
        final redirect = Uri.encodeQueryComponent(state.uri.toString());
        return '/login?redirect=$redirect';
      }
      final hasUserRole = session.user?.roles.contains('USER') ?? false;
      if (!hasUserRole) {
        return location == '/forbidden' ? null : '/forbidden';
      }
      if (location == '/forbidden') return '/';
      if (isAuthPage || location == '/splash') {
        return _safeRedirect(state.uri.queryParameters['redirect']) ?? '/';
      }
      return null;
    },
    routes: [
      GoRoute(path: '/splash', builder: (context, state) => const SplashPage()),
      GoRoute(
        path: '/login',
        builder: (context, state) => LoginPage(
          passwordChanged: state.uri.queryParameters['passwordChanged'] == '1',
        ),
      ),
      GoRoute(
        path: '/register',
        builder: (context, state) => const RegisterPage(),
      ),
      GoRoute(
        path: '/forbidden',
        builder: (context, state) => const ForbiddenPage(),
      ),
      ...appModuleRoutes,
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});

String? _safeRedirect(String? value) {
  if (value == null || !value.startsWith('/') || value.startsWith('//')) {
    return null;
  }
  return value;
}
