import 'package:shopping_app/core/network/api_exception.dart';

typedef JsonMap = Map<String, dynamic>;

JsonMap requireJsonMap(Object? value, {String message = '服务端响应格式错误'}) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) {
    return value.map((key, item) => MapEntry(key.toString(), item));
  }
  throw ApiException(message: message);
}

T unwrapApiResponse<T>(Object? body, T Function(Object? data) parse) {
  final json = requireJsonMap(body);
  final code = json['code'];
  final message = json['message']?.toString();
  if (code is! num) {
    throw const ApiException(message: '服务端响应缺少业务状态码');
  }
  if (code.toInt() != 0) {
    throw ApiException(
      message: message?.isNotEmpty == true ? message! : '请求失败',
      businessCode: code.toInt(),
    );
  }
  return parse(json['data']);
}

List<String> readStringList(Object? value) {
  if (value is! List) return const [];
  return value.map((item) => item.toString()).toList(growable: false);
}
