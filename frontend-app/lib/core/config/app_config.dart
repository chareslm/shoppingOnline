abstract final class AppConfig {
  static const _configuredApiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  static String get apiBaseUrl =>
      _configuredApiBaseUrl.replaceFirst(RegExp(r'/+$'), '');
}
