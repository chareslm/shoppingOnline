import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/account/domain/statistics_models.dart';

class StatisticsPage extends ConsumerStatefulWidget {
  const StatisticsPage({super.key});

  @override
  ConsumerState<StatisticsPage> createState() => _StatisticsPageState();
}

class _StatisticsPageState extends ConsumerState<StatisticsPage> {
  late DateTime _startAt;
  late DateTime _endAt;
  UserStatisticsOverview? _overview;
  String? _error;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _startAt = DateTime(
      now.year,
      now.month,
      now.day,
    ).subtract(const Duration(days: 29));
    _endAt = now;
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await ref
          .read(statisticsApiProvider)
          .userOverview(startAt: _startAt, endAt: _endAt);
      if (mounted) setState(() => _overview = result);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _selectRange() async {
    final selected = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
      initialDateRange: DateTimeRange(
        start: _startAt,
        end: _endAt.isAfter(DateTime.now()) ? DateTime.now() : _endAt,
      ),
      helpText: '选择最多 31 个自然日',
    );
    if (selected == null) return;
    final days = selected.end.difference(selected.start).inDays + 1;
    if (days > 31) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('统计时间范围最多覆盖 31 个自然日')));
      }
      return;
    }
    setState(() {
      _startAt = DateTime(
        selected.start.year,
        selected.start.month,
        selected.start.day,
      );
      _endAt = DateTime(
        selected.end.year,
        selected.end.month,
        selected.end.day,
      ).add(const Duration(days: 1));
    });
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final metrics = _overview?.metrics;
    final cards = metrics == null
        ? const <_MetricCard>[]
        : [
            _MetricCard(
              '支付订单',
              metrics.paidOrderCount,
              Icons.receipt_long_outlined,
            ),
            _MetricCard(
              '支付总额',
              '¥${metrics.grossPaidAmount}',
              Icons.payments_outlined,
            ),
            _MetricCard(
              '成功退款',
              '¥${metrics.successfulRefundAmount}',
              Icons.undo_outlined,
            ),
            _MetricCard(
              '有效评价',
              metrics.displayedReviewCount,
              Icons.rate_review_outlined,
            ),
          ];
    return Scaffold(
      appBar: AppBar(
        title: const Text('消费统计'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _selectRange,
            tooltip: '选择时间范围',
            icon: const Icon(Icons.date_range_outlined),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              '${_date(_startAt)} 至 ${_date(_endAt.subtract(const Duration(days: 1)))}',
            ),
            const SizedBox(height: 8),
            const Text('仅查询当前账号本人数据，金额来自 MySQL 权威业务表。'),
            if (_error != null) ...[
              const SizedBox(height: 16),
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            if (_loading && _overview == null) ...[
              const SizedBox(height: 36),
              const Center(child: CircularProgressIndicator()),
            ] else ...[
              const SizedBox(height: 20),
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                  childAspectRatio: 1.3,
                ),
                itemCount: cards.length,
                itemBuilder: (context, index) => cards[index],
              ),
              const SizedBox(height: 20),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(18),
                  child: Text(
                    '支付总额按原支付成功时间统计；成功退款按退款实际成功时间统计，可能来自不同支付周期。本页面不是账单或结算凭证。\n\n'
                    '口径 ${_overview?.metricVersion ?? '—'} · ${_overview?.timezone ?? '—'}',
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard(this.label, this.value, this.icon);

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: Theme.of(context).colorScheme.primary),
          const Spacer(),
          Text(value, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 4),
          Text(label),
        ],
      ),
    ),
  );
}

String _date(DateTime value) =>
    '${value.year}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';
