import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class OrderDetailPage extends ConsumerStatefulWidget {
  const OrderDetailPage({super.key, required this.orderId});

  final String orderId;

  @override
  ConsumerState<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends ConsumerState<OrderDetailPage> {
  Order? _order;
  bool _loading = true;
  bool _acting = false;
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
      final order = await ref.read(tradeApiProvider).orderDetail(widget.orderId);
      if (mounted) setState(() => _order = order);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _run(Future<void> Function() action) async {
    if (_acting) return;
    setState(() => _acting = true);
    try {
      await action();
      await _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _acting = false);
    }
  }

  Future<void> _pay() async {
    final order = _order;
    if (order == null) return;
    try {
      final payment = await ref
          .read(tradeApiProvider)
          .createPayment(order.orderId);
      if (mounted) context.push('/pay/${payment.paymentOrderId}');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }

  Future<void> _refund() async {
    final order = _order;
    if (order == null) return;
    final controller = TextEditingController(text: order.payAmount.toStringAsFixed(2));
    final reason = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('申请退款'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: controller,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: '退款金额'),
            ),
            TextField(
              controller: reason,
              decoration: const InputDecoration(labelText: '原因（可选）'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('提交'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    final amount = double.tryParse(controller.text.trim());
    await _run(
      () => ref
          .read(tradeApiProvider)
          .createRefund(
            orderId: order.orderId,
            amount: amount,
            reason: reason.text.trim(),
          ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final order = _order;
    return Scaffold(
      appBar: AppBar(title: const Text('订单详情')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text('订单号 ${order?.orderNo ?? ''}'),
                Text(orderStatusLabels[order?.status] ?? ''),
                Text(formatMoney(order?.payAmount)),
                Text(
                  '${order?.receiverName} ${order?.receiverPhone}\n${order?.receiverAddress}',
                ),
                const Divider(),
                for (final item in order?.items ?? const [])
                  ListTile(
                    title: Text(item.skuName ?? '商品'),
                    trailing: Text(
                      '${formatMoney(item.price)} × ${item.quantity}',
                    ),
                  ),
                const SizedBox(height: 16),
                if (order?.status == 0)
                  FilledButton(
                    onPressed: _acting ? null : _pay,
                    child: const Text('去支付'),
                  ),
                if (order?.status == 0)
                  TextButton(
                    onPressed: _acting
                        ? null
                        : () => _run(
                            () => ref
                                .read(tradeApiProvider)
                                .cancelOrder(order!.orderId),
                          ),
                    child: const Text('取消订单'),
                  ),
                if (order?.status == 2)
                  FilledButton(
                    onPressed: _acting
                        ? null
                        : () => _run(
                            () => ref
                                .read(tradeApiProvider)
                                .confirmOrder(order!.orderId),
                          ),
                    child: const Text('确认收货'),
                  ),
                if (order != null &&
                    (order.status == 1 || order.status == 2 || order.status == 3))
                  OutlinedButton(
                    onPressed: _acting ? null : _refund,
                    child: const Text('申请退款'),
                  ),
              ],
            ),
    );
  }
}
