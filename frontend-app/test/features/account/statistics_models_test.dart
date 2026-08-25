import 'package:flutter_test/flutter_test.dart';
import 'package:shopping_app/features/account/domain/statistics_models.dart';

void main() {
  test('解析本人统计响应并保持金额和计数字符串', () {
    final overview = UserStatisticsOverview.fromJson({
      'metricVersion': 'v1',
      'timezone': 'Asia/Shanghai',
      'dataAsOf': '2026-08-25T12:00:00+08:00',
      'metrics': {
        'paidOrderCount': '9007199254740993',
        'grossPaidAmount': '120.30',
        'successfulRefundAmount': '20.00',
        'displayedReviewCount': '3',
      },
    });

    expect(overview.metrics.paidOrderCount, '9007199254740993');
    expect(overview.metrics.grossPaidAmount, '120.30');
    expect(overview.metrics.successfulRefundAmount, '20.00');
    expect(overview.timezone, 'Asia/Shanghai');
  });
}
