import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class CheckoutPage extends ConsumerStatefulWidget {
  const CheckoutPage({super.key});

  @override
  ConsumerState<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends ConsumerState<CheckoutPage> {
  List<CartItem> _items = const [];
  List<UserAddress> _addresses = const [];
  String? _addressId;
  final _remarkController = TextEditingController();
  bool _loading = true;
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  @override
  void dispose() {
    _remarkController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final cart = await ref.read(tradeApiProvider).getCart();
      final addresses = await ref.read(userRepositoryProvider).addresses();
      final checked = cart.groups
          .expand((group) => group.items)
          .where((item) => item.checked == 1)
          .toList(growable: false);
      String? addressId;
      for (final address in addresses) {
        if (address.isDefault) {
          addressId = address.id;
          break;
        }
      }
      if (!mounted) return;
      setState(() {
        _items = checked;
        _addresses = addresses;
        _addressId = addressId ?? (addresses.isEmpty ? null : addresses.first.id);
      });
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submit() async {
    UserAddress? address;
    for (final item in _addresses) {
      if (item.id == _addressId) {
        address = item;
        break;
      }
    }
    if (address == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请先选择收货地址')));
      return;
    }
    if (_items.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('没有可结算的商品，请返回购物车勾选')));
      return;
    }
    setState(() => _submitting = true);
    try {
      final orders = await ref
          .read(tradeApiProvider)
          .createOrder(
            receiverName: address.recipientName,
            receiverPhone: address.recipientPhone,
            receiverAddress:
                '${address.provinceName} ${address.cityName} ${address.districtName} ${address.detailAddress}'
                    .trim(),
            remark: _remarkController.text.trim(),
          );
      final payment = await ref
          .read(tradeApiProvider)
          .createPayment(orders.first.orderId);
      if (mounted) context.go('/pay/${payment.paymentOrderId}');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final total = _items.fold<double>(
      0,
      (sum, item) => sum + item.price * item.quantity,
    );
    return Scaffold(
      appBar: AppBar(title: const Text('确认订单')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text('收货地址', style: Theme.of(context).textTheme.titleMedium),
                if (_addresses.isEmpty)
                  ListTile(
                    title: const Text('暂无收货地址'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => context.push('/addresses'),
                  )
                else
                  for (final address in _addresses)
                    RadioListTile<String>(
                      value: address.id,
                      groupValue: _addressId,
                      onChanged: (value) => setState(() => _addressId = value),
                      title: Text(
                        '${address.recipientName}  ${address.recipientPhone}',
                      ),
                      subtitle: Text(
                        '${address.provinceName}${address.cityName}${address.districtName}${address.detailAddress}',
                      ),
                    ),
                const SizedBox(height: 12),
                for (final item in _items)
                  ListTile(
                    title: Text(item.skuName ?? '商品'),
                    trailing: Text(
                      '${formatMoney(item.price)} × ${item.quantity}',
                    ),
                  ),
                TextField(
                  controller: _remarkController,
                  decoration: const InputDecoration(labelText: '备注（可选）'),
                ),
                const SizedBox(height: 16),
                Text('合计 ${formatMoney(total)}'),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _submitting ? null : _submit,
                  child: Text(_submitting ? '提交中…' : '提交订单并支付'),
                ),
              ],
            ),
    );
  }
}
