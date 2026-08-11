class ApiException implements Exception {
  const ApiException({
    required this.message,
    this.statusCode,
    this.businessCode,
    this.cause,
  });

  final String message;
  final int? statusCode;
  final int? businessCode;
  final Object? cause;

  bool get isUnauthorized => statusCode == 401;

  @override
  String toString() => message;
}
