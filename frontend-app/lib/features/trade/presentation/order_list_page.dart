import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class OrderListPage extends ConsumerStatefulWidget {
  const OrderListPage({super.key});

  @override
  ConsumerState<OrderListPage> createState() => _OrderListPageState();
}

class _OrderListPageState extends ConsumerState<OrderListPage> {
  List<Order> _orders = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final orders = await ref.read(tradeApiProvider).listOrders();
      if (mounted) setState(() => _orders = orders);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('我的订单')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: _loading
          ? const ListView(
              children: [
                SizedBox(height: 80),
                Center(child: CircularProgressIndicator()),
              ],
            )
          : _error != null
          ? ListView(children: [ListTile(title: Text(_error!))])
          : _orders.isEmpty
          ? const ListView(children: [Center(child: Text('暂无订单'))])
          : ListView.builder(
              itemCount: _orders.length,
              itemBuilder: (context, index) {
                final order = _orders[index];
                return ListTile(
                  title: Text(order.orderNo),
                  subtitle: Text(
                    '${orderStatusLabels[order.status] ?? order.status}  ${formatMoney(order.payAmount)}',
                  ),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/orders/${order.orderId}'),
                );
              },
            ),
    ),
  );
}
