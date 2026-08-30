import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class PaymentPage extends ConsumerStatefulWidget {
  const PaymentPage({super.key, required this.paymentOrderId});

  final String paymentOrderId;

  @override
  ConsumerState<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends ConsumerState<PaymentPage> {
  PaymentOrder? _payment;
  bool _loading = true;
  bool _paying = false;
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
      final payment = await ref
          .read(tradeApiProvider)
          .paymentDetail(widget.paymentOrderId);
      if (mounted) setState(() => _payment = payment);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _mockPay() async {
    setState(() => _paying = true);
    try {
      final payment = await ref
          .read(tradeApiProvider)
          .mockPay(widget.paymentOrderId);
      if (!mounted) return;
      setState(() => _payment = payment);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('支付成功')));
      context.go('/orders/${payment.orderId}');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _paying = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final payment = _payment;
    return Scaffold(
      appBar: AppBar(title: const Text('支付')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('支付单 ${payment?.paymentNo ?? ''}'),
                  const SizedBox(height: 8),
                  Text(formatMoney(payment?.amount)),
                  const SizedBox(height: 8),
                  Text(paymentStatusLabels[payment?.status] ?? ''),
                  const Spacer(),
                  if (payment?.status == 0)
                    FilledButton(
                      onPressed: _paying ? null : _mockPay,
                      child: Text(_paying ? '支付中…' : '模拟支付'),
                    ),
                ],
              ),
            ),
    );
  }
}
