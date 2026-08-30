import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module_registry.dart';

void main() {
  test('注册五个约定模块且业务模块包含约定路径', () {
    expect(appModules.map((module) => module.key), [
      'account',
      'merchant',
      'product',
      'trade',
      'message',
    ]);

    List<String> pathsOf(String key) => appModules
        .firstWhere((module) => module.key == key)
        .routes
        .whereType<GoRoute>()
        .map((route) => route.path)
        .toList();

    expect(pathsOf('product'), containsAll([
      '/products',
      '/products/:spuId',
      '/merchant/products',
      '/merchant/add-product',
    ]));
    expect(pathsOf('trade'), containsAll([
      '/cart',
      '/checkout',
      '/orders',
      '/orders/:orderId',
      '/pay/:paymentOrderId',
    ]));
    expect(pathsOf('merchant'), containsAll([
      '/merchant',
      '/merchant/staff',
      '/merchant/stats',
    ]));
    expect(pathsOf('message'), containsAll([
      '/chat',
      '/chat/:sessionId',
      '/notifications',
      '/merchant/inbox',
    ]));
    expect(
      appModules.first.routes.any(
        (route) => route is GoRoute && route.path == '/statistics',
      ),
      isTrue,
    );
  });
}
