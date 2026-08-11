import 'package:flutter_test/flutter_test.dart';
import 'package:shopping_app/app/module_registry.dart';

void main() {
  test('注册五个约定模块且业务模块保持空路由', () {
    expect(appModules.map((module) => module.key), [
      'account',
      'merchant',
      'product',
      'trade',
      'message',
    ]);
    for (final module in appModules.where(
      (module) => module.key != 'account',
    )) {
      expect(module.routes, isEmpty, reason: '${module.key} 不应提前注册业务页面');
    }
  });
}
