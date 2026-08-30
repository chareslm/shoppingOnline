import 'dart:async';

import 'package:dio/dio.dart';
import 'package:shopping_app/core/auth/session_controller.dart';
import 'package:shopping_app/core/network/api_exception.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/account/domain/auth_models.dart';

class ApiClient {
  ApiClient(String baseUrl, this._sessionController)
    : _dio = Dio(_options(baseUrl)),
      _refreshDio = Dio(_options(baseUrl)) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final accessToken = _sessionController.accessToken;
          if (accessToken != null && accessToken.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $accessToken';
          }
          handler.next(options);
        },
        onError: _handleError,
      ),
    );
  }

  static BaseOptions _options(String baseUrl) => BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
    sendTimeout: const Duration(seconds: 10),
    contentType: Headers.jsonContentType,
    responseType: ResponseType.json,
  );

  final Dio _dio;
  final Dio _refreshDio;
  final AuthSessionController _sessionController;
  Future<String>? _refreshFuture;

  Future<Response<dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) =>
      _request(() => _dio.get<dynamic>(path, queryParameters: queryParameters));

  Future<Response<dynamic>> post(
    String path, {
    Object? data,
    Options? options,
  }) => _request(() => _dio.post<dynamic>(path, data: data, options: options));

  Future<Response<dynamic>> postMultipart(String path, FormData data) =>
      _request(
        () => _dio.post<dynamic>(
          path,
          data: data,
          options: Options(
            contentType: Headers.multipartFormDataContentType,
            sendTimeout: const Duration(seconds: 30),
            receiveTimeout: const Duration(seconds: 30),
          ),
        ),
      );

  Future<Response<dynamic>> put(String path, {Object? data}) =>
      _request(() => _dio.put<dynamic>(path, data: data));

  Future<Response<dynamic>> delete(String path, {Object? data}) =>
      _request(() => _dio.delete<dynamic>(path, data: data));

  Future<Response<dynamic>> _request(
    Future<Response<dynamic>> Function() request,
  ) async {
    try {
      return await request();
    } on DioException catch (error) {
      throw toApiException(error);
    }
  }

  Future<void> _handleError(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final request = error.requestOptions;
    final shouldRefresh =
        error.response?.statusCode == 401 &&
        request.extra['authRetried'] != true &&
        !_isTokenIssuingRequest(request.path) &&
        _sessionController.refreshToken != null;

    if (!shouldRefresh) {
      handler.next(error);
      return;
    }

    request.extra['authRetried'] = true;
    try {
      final accessToken = await _refreshOnce();
      request.headers['Authorization'] = 'Bearer $accessToken';
      handler.resolve(await _dio.fetch<dynamic>(request));
    } catch (refreshError) {
      await _sessionController.clear();
      handler.reject(
        DioException(
          requestOptions: request,
          response: error.response,
          error: refreshError,
          type: DioExceptionType.badResponse,
          message: '登录会话已失效，请重新登录',
        ),
      );
    }
  }

  bool _isTokenIssuingRequest(String path) =>
      path.contains('/api/auth/register') ||
      path.contains('/api/auth/login/password') ||
      path.contains('/api/auth/refresh');

  Future<String> _refreshOnce() {
    final running = _refreshFuture;
    if (running != null) return running;

    final created = _refreshAccessToken();
    _refreshFuture = created;
    unawaited(
      created.then<void>(
        (_) => _clearRefreshFuture(created),
        onError: (Object _, StackTrace _) => _clearRefreshFuture(created),
      ),
    );
    return created;
  }

  void _clearRefreshFuture(Future<String> completed) {
    if (identical(_refreshFuture, completed)) _refreshFuture = null;
  }

  Future<String> _refreshAccessToken() async {
    final refreshToken = _sessionController.refreshToken;
    if (refreshToken == null || refreshToken.isEmpty) {
      throw const ApiException(message: '登录会话不存在', statusCode: 401);
    }

    try {
      final response = await _refreshDio.post<dynamic>(
        '/api/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      final result = unwrapApiResponse(response.data, LoginResult.fromJson);
      await _sessionController.updateTokens(
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
      );
      return result.accessToken;
    } on DioException catch (error) {
      throw toApiException(error);
    }
  }

  ApiException toApiException(DioException error) {
    final nested = error.error;
    if (nested is ApiException) return nested;

    final response = error.response;
    final body = response?.data;
    if (body is Map) {
      final json = requireJsonMap(body);
      final code = json['code'];
      final message = json['message']?.toString();
      return ApiException(
        message: message?.isNotEmpty == true ? message! : '请求失败',
        statusCode: response?.statusCode,
        businessCode: code is num ? code.toInt() : null,
        cause: error,
      );
    }

    final message = switch (error.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout ||
      DioExceptionType.receiveTimeout => '请求超时，请检查网络后重试',
      DioExceptionType.connectionError => '无法连接服务器，请检查网络和 API 地址',
      _ => error.message ?? '请求失败，请稍后重试',
    };
    return ApiException(
      message: message,
      statusCode: response?.statusCode,
      cause: error,
    );
  }
}
