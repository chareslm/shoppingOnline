import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/merchant/domain/merchant_models.dart';

class MerchantStatsPage extends ConsumerStatefulWidget {
  const MerchantStatsPage({super.key});

  @override
  ConsumerState<MerchantStatsPage> createState() => _MerchantStatsPageState();
}

class _MerchantStatsPageState extends ConsumerState<MerchantStatsPage> {
  ShopStatisticsOverview? _overview;
  bool _loading = true;
  String? _error;
  late DateTime _startAt;
  late DateTime _endAt;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _endAt = DateTime(now.year, now.month, now.day).add(const Duration(days: 1));
    _startAt = _endAt.subtract(const Duration(days: 7));
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final overview = await ref
          .read(merchantApiProvider)
          .statisticsOverview(startAt: _startAt, endAt: _endAt);
      if (mounted) setState(() => _overview = overview);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final metrics = _overview?.metrics;
    return Scaffold(
      appBar: AppBar(title: const Text('经营统计')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            const Text('最近 7 个自然日，金额来自 MySQL 权威业务表。'),
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            if (_loading && _overview == null)
              const Padding(
                padding: EdgeInsets.only(top: 48),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (metrics != null) ...[
              const SizedBox(height: 16),
              _tile('支付订单', metrics.paidOrderCount),
              _tile('支付买家', metrics.paidBuyerCount),
              _tile('支付总额', '¥${metrics.grossPaidAmount}'),
              _tile('成功退款', '¥${metrics.successfulRefundAmount}'),
              _tile('净收款活动额', '¥${metrics.netCashflowActivity}'),
              _tile('售出件数', metrics.soldQuantity),
              _tile('在售商品快照', metrics.onSaleProductSnapshot),
              _tile('有效评价', metrics.displayedReviewCount),
              const SizedBox(height: 12),
              Text(
                '${_overview?.shopName ?? ''} · ${_overview?.metricVersion ?? ''} · ${_overview?.timezone ?? ''}',
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _tile(String label, String value) => Card(
    child: ListTile(title: Text(label), trailing: Text(value)),
  );
}
