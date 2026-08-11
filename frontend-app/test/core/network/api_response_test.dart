import 'package:flutter_test/flutter_test.dart';
import 'package:shopping_app/core/network/api_exception.dart';
import 'package:shopping_app/core/network/api_response.dart';

void main() {
  group('unwrapApiResponse', () {
    test('返回成功响应的数据', () {
      final result = unwrapApiResponse<int>({
        'code': 0,
        'message': 'success',
        'data': 42,
      }, (data) => data as int);

      expect(result, 42);
    });

    test('保留业务错误码和消息', () {
      expect(
        () => unwrapApiResponse<void>({
          'code': 40901,
          'message': '用户名已存在',
          'data': null,
        }, (_) {}),
        throwsA(
          isA<ApiException>()
              .having((error) => error.businessCode, 'businessCode', 40901)
              .having((error) => error.message, 'message', '用户名已存在'),
        ),
      );
    });
  });
}
