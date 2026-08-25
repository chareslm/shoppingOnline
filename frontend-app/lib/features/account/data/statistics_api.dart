import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/account/domain/statistics_models.dart';

class StatisticsApi {
  StatisticsApi(this._client);

  final ApiClient _client;

  Future<UserStatisticsOverview> userOverview({
    required DateTime startAt,
    required DateTime endAt,
  }) async {
    final response = await _client.get(
      '/api/users/me/statistics/overview',
      queryParameters: {
        'startAt': _localDateTime(startAt),
        'endAt': _localDateTime(endAt),
        'timezone': 'Asia/Shanghai',
        'granularity': 'DAY',
      },
    );
    return unwrapApiResponse(response.data, UserStatisticsOverview.fromJson);
  }
}

String _localDateTime(DateTime value) {
  String two(int part) => part.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)}T'
      '${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
}
