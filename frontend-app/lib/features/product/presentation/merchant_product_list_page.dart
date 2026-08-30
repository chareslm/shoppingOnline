import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';

class MerchantProductListPage extends ConsumerStatefulWidget {
  const MerchantProductListPage({super.key});

  @override
  ConsumerState<MerchantProductListPage> createState() =>
      _MerchantProductListPageState();
}

class _MerchantProductListPageState
    extends ConsumerState<MerchantProductListPage> {
  List<SpuItem> _items = const [];
  bool _loading = false;
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
      final result = await ref.read(productApiProvider).merchantPage();
      if (mounted) setState(() => _items = result.items);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _changeStatus(SpuItem item, String action) async {
    try {
      await ref.read(productApiProvider).changeStatus(item.id, action);
      await _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }

  Future<void> _adjustStock(SpuItem item) async {
    try {
      final detail = await ref.read(productApiProvider).merchantDetail(item.id);
      if (!mounted || detail.skus.isEmpty) return;
      final drafts = {
        for (final sku in detail.skus) sku.id: TextEditingController(text: '${sku.availableStock}'),
      };
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('调整库存'),
          content: SizedBox(
            width: 360,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  for (final sku in detail.skus)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: TextField(
                        controller: drafts[sku.id],
                        keyboardType: TextInputType.number,
                        decoration: InputDecoration(
                          labelText: formatSkuAttributes(sku.attributes),
                          helperText: '当前 ${sku.availableStock}',
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('保存'),
            ),
          ],
        ),
      );
      if (confirmed != true) return;
      for (final sku in detail.skus) {
        final next = int.tryParse(drafts[sku.id]?.text.trim() ?? '');
        if (next == null || next < 0 || next == sku.availableStock) continue;
        await ref.read(productApiProvider).adjustStock(sku.id, next - sku.availableStock);
      }
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('库存已更新')));
      }
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
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('本店商品')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_error != null)
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          if (_loading && _items.isEmpty)
            const Padding(
              padding: EdgeInsets.only(top: 48),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_items.isEmpty && !_loading)
            const Padding(
              padding: EdgeInsets.only(top: 48),
              child: Center(child: Text('暂无本店商品')),
            )
          else
            for (final item in _items)
              Card(
                child: ListTile(
                  title: Text(item.name),
                  subtitle: Text(
                    '${spuStatusLabels[item.status] ?? item.status}  · ${formatMoney(item.priceMin)}',
                  ),
                  trailing: PopupMenuButton<String>(
                    onSelected: (value) {
                      if (value == 'stock') {
                        _adjustStock(item);
                      } else {
                        _changeStatus(item, value);
                      }
                    },
                    itemBuilder: (context) => const [
                      PopupMenuItem(value: 'SUBMIT', child: Text('提交审核')),
                      PopupMenuItem(value: 'PUBLISH', child: Text('上架')),
                      PopupMenuItem(value: 'OFF_SHELF', child: Text('下架')),
                      PopupMenuItem(value: 'stock', child: Text('调整库存')),
                    ],
                  ),
                ),
              ),
        ],
      ),
    ),
  );
}
