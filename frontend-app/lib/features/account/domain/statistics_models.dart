import 'package:shopping_app/core/network/api_response.dart';

class UserStatisticsOverview {
  const UserStatisticsOverview({
    required this.metricVersion,
    required this.timezone,
    required this.dataAsOf,
    required this.metrics,
  });

  factory UserStatisticsOverview.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return UserStatisticsOverview(
      metricVersion: json['metricVersion'].toString(),
      timezone: json['timezone'].toString(),
      dataAsOf: DateTime.parse(json['dataAsOf'].toString()),
      metrics: UserStatisticsMetrics.fromJson(json['metrics']),
    );
  }

  final String metricVersion;
  final String timezone;
  final DateTime dataAsOf;
  final UserStatisticsMetrics metrics;
}

class UserStatisticsMetrics {
  const UserStatisticsMetrics({
    required this.paidOrderCount,
    required this.grossPaidAmount,
    required this.successfulRefundAmount,
    required this.displayedReviewCount,
  });

  factory UserStatisticsMetrics.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return UserStatisticsMetrics(
      paidOrderCount: json['paidOrderCount'].toString(),
      grossPaidAmount: json['grossPaidAmount'].toString(),
      successfulRefundAmount: json['successfulRefundAmount'].toString(),
      displayedReviewCount: json['displayedReviewCount'].toString(),
    );
  }

  final String paidOrderCount;
  final String grossPaidAmount;
  final String successfulRefundAmount;
  final String displayedReviewCount;
}
