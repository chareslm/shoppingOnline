import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class CartPage extends ConsumerStatefulWidget {
  const CartPage({super.key});

  @override
  ConsumerState<CartPage> createState() => _CartPageState();
}

class _CartPageState extends ConsumerState<CartPage> {
  Cart? _cart;
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
      final cart = await ref.read(tradeApiProvider).getCart();
      if (mounted) setState(() => _cart = cart);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _run(Future<void> Function() action) async {
    try {
      await action();
      await _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final groups = _cart?.groups ?? const [];
    return Scaffold(
      appBar: AppBar(title: const Text('购物车')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (groups.isEmpty) const Center(child: Text('购物车是空的')),
                  for (final group in groups) ...[
                    Text(
                      group.shopName ?? '店铺',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    for (final item in group.items)
                      CheckboxListTile(
                        value: item.checked == 1,
                        onChanged: (checked) => _run(
                          () => ref
                              .read(tradeApiProvider)
                              .updateChecked(item.itemId, checked ?? false),
                        ),
                        title: Text(item.skuName ?? '商品'),
                        subtitle: Text(
                          '${formatMoney(item.price)}  × ${item.quantity}',
                        ),
                        secondary: IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () => _run(
                            () => ref
                                .read(tradeApiProvider)
                                .removeItem(item.itemId),
                          ),
                        ),
                      ),
                    for (final item in group.items)
                      Align(
                        alignment: Alignment.centerRight,
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              onPressed: item.quantity <= 1
                                  ? null
                                  : () => _run(
                                      () => ref
                                          .read(tradeApiProvider)
                                          .updateQuantity(
                                            item.itemId,
                                            item.quantity - 1,
                                          ),
                                    ),
                              icon: const Icon(Icons.remove),
                            ),
                            IconButton(
                              onPressed: () => _run(
                                () => ref
                                    .read(tradeApiProvider)
                                    .updateQuantity(
                                      item.itemId,
                                      item.quantity + 1,
                                    ),
                              ),
                              icon: const Icon(Icons.add),
                            ),
                          ],
                        ),
                      ),
                    const Divider(),
                  ],
                ],
              ),
            ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton(
            onPressed: groups.any(
              (group) => group.items.any((item) => item.checked == 1),
            )
                ? () => context.push('/checkout')
                : null,
            child: const Text('去结算'),
          ),
        ),
      ),
    );
  }
}
