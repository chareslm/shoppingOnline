import 'dart:convert';

String? validateStrongPassword(String? value) {
  final password = value ?? '';
  if (password.length < 12 || password.length > 64) {
    return '密码长度须为 12–64 位';
  }
  if (utf8.encode(password).length > 72 ||
      !RegExp(r'[A-Z]').hasMatch(password) ||
      !RegExp(r'[a-z]').hasMatch(password) ||
      !RegExp(r'[0-9]').hasMatch(password) ||
      !RegExp(r'[^A-Za-z0-9]').hasMatch(password)) {
    return '密码须包含大小写字母、数字和特殊字符';
  }
  return null;
}
