import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:file_picker/file_picker.dart';
import 'package:http_parser/http_parser.dart';
import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_exception.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/merchant/domain/merchant_models.dart';

class MerchantApi {
  MerchantApi(this._client);

  final ApiClient _client;

  Future<MerchantApplicationReceipt> submitApplication(
    MerchantApplicationRequest application,
    List<PlatformFile> files,
  ) async {
    final form = FormData();
    form.files.add(
      MapEntry(
        'application',
        MultipartFile.fromBytes(
          utf8.encode(jsonEncode(application.toJson())),
          filename: 'application.json',
          contentType: MediaType('application', 'json'),
        ),
      ),
    );
    for (final file in files) {
      final path = file.path;
      if (path == null || path.isEmpty) {
        throw const ApiException(message: '无法读取所选资质文件');
      }
      form.files.add(
        MapEntry(
          'files',
          await MultipartFile.fromFile(path, filename: file.name),
        ),
      );
    }
    final response = await _client.postMultipart(
      '/api/merchant/applications',
      form,
    );
    return unwrapApiResponse(
      response.data,
      MerchantApplicationReceipt.fromJson,
    );
  }

  Future<ShopSummary> currentShop() async {
    final response = await _client.get('/api/merchant/shop');
    return unwrapApiResponse(response.data, ShopSummary.fromJson);
  }

  Future<List<ShopStaffAccount>> listStaff() async {
    final response = await _client.get('/api/merchant/staff');
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, ShopStaffAccount.fromJson, message: '客服账号响应格式错误'),
    );
  }

  Future<ShopStaffAccount> createStaff({
    required String email,
    required String displayName,
    String? username,
  }) async {
    final response = await _client.post(
      '/api/merchant/staff',
      data: {
        'email': email,
        'displayName': displayName,
        if (username != null && username.isNotEmpty) 'username': username,
      },
    );
    return unwrapApiResponse(response.data, ShopStaffAccount.fromJson);
  }

  Future<ShopStaffAccount> retryStaffEmail(String staffId) async {
    final response = await _client.post(
      '/api/merchant/staff/$staffId/credential-email/retry',
    );
    return unwrapApiResponse(response.data, ShopStaffAccount.fromJson);
  }

  Future<ShopStatisticsOverview> statisticsOverview({
    required DateTime startAt,
    required DateTime endAt,
  }) async {
    final response = await _client.get(
      '/api/merchant/statistics/overview',
      queryParameters: {
        'startAt': _localDateTime(startAt),
        'endAt': _localDateTime(endAt),
        'timezone': 'Asia/Shanghai',
        'granularity': 'DAY',
      },
    );
    return unwrapApiResponse(response.data, ShopStatisticsOverview.fromJson);
  }
}

String _localDateTime(DateTime value) {
  String two(int part) => part.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)}T'
      '${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
}
