import 'package:flutter_test/flutter_test.dart';
import 'package:shopping_app/core/validation/password_validator.dart';

void main() {
  test('接受符合后端策略的强密码', () {
    expect(validateStrongPassword('ValidPassword1!'), isNull);
  });

  test('拒绝缺少字符类型的密码', () {
    expect(validateStrongPassword('onlylowercase1'), isNotNull);
  });
}
