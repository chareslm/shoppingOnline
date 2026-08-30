import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/config/app_config.dart';
import 'package:shopping_app/core/media/media_url.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class ProductDetailPage extends ConsumerStatefulWidget {
  const ProductDetailPage({super.key, required this.spuId});

  final String spuId;

  @override
  ConsumerState<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends ConsumerState<ProductDetailPage> {
  SpuDetail? _detail;
  ReviewStats? _stats;
  List<Review> _reviews = const [];
  Sku? _sku;
  Map<String, String> _selectedAttrs = const {};
  int _quantity = 1;
  bool _loading = true;
  bool _adding = false;
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
      final api = ref.read(productApiProvider);
      final detail = await api.spuDetail(widget.spuId);
      ReviewStats? stats;
      List<Review> reviews = const [];
      try {
        stats = await api.reviewStats(widget.spuId);
        reviews = (await api.reviews(widget.spuId)).items;
      } catch (_) {}
      if (!mounted) return;
      setState(() {
        _detail = detail;
        _stats = stats;
        _reviews = reviews;
        _sku = detail.skus.isEmpty ? null : detail.skus.first;
        _selectedAttrs = selectedAttrsFromSku(_sku);
      });
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _addToCart() async {
    final detail = _detail;
    final sku = _sku;
    if (detail == null || sku == null) return;
    if (sku.availableStock < _quantity) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('库存不足')));
      return;
    }
    setState(() => _adding = true);
    try {
      await ref
          .read(tradeApiProvider)
          .addCartItem(
            AddCartItemRequest(
              skuId: sku.id,
              quantity: _quantity,
              shopId: detail.shopId,
              price: sku.price,
            ),
          );
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('已加入购物车')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _adding = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    return Scaffold(
      appBar: AppBar(title: Text(detail?.name ?? '商品详情')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (detail?.mainImage != null)
                  AspectRatio(
                    aspectRatio: 1.4,
                    child: Image.network(
                      mediaUrl(AppConfig.apiBaseUrl, detail!.mainImage),
                      fit: BoxFit.cover,
                      errorBuilder: (_, _, _) =>
                          const Icon(Icons.broken_image_outlined, size: 64),
                    ),
                  ),
                const SizedBox(height: 12),
                Text(
                  detail?.name ?? '',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 8),
                Text(formatMoney(_sku?.price ?? detail?.priceMin)),
                Text('店铺：${detail?.shopName ?? '—'}  · 销量 ${detail?.sales ?? 0}'),
                if (_stats != null)
                  Text(
                    '评分 ${_stats!.averageRating.toStringAsFixed(1)}（${_stats!.totalCount} 条评价）',
                  ),
                const SizedBox(height: 16),
                if (detail != null && detail.skus.isNotEmpty) ...[
                  for (final axis in specAxesFromSkus(detail.skus, _selectedAttrs)) ...[
                    Text(axis.name, style: Theme.of(context).textTheme.titleSmall),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: [
                        for (final option in axis.values)
                          ChoiceChip(
                            label: Text(option.value),
                            selected: option.selected,
                            onSelected: (_) => setState(() {
                              _selectedAttrs = {..._selectedAttrs, axis.name: option.value};
                              _sku = matchSku(detail.skus, _selectedAttrs);
                              if (_sku != null) {
                                _selectedAttrs = selectedAttrsFromSku(_sku);
                              }
                            }),
                          ),
                      ],
                    ),
                    const SizedBox(height: 8),
                  ],
                  if (specAxesFromSkus(detail.skus, _selectedAttrs).isEmpty)
                    Wrap(
                      spacing: 8,
                      children: [
                        for (final sku in detail.skus)
                          ChoiceChip(
                            label: Text('${formatSkuAttributes(sku.attributes)}  ${formatMoney(sku.price)}'),
                            selected: _sku?.id == sku.id,
                            onSelected: (_) => setState(() {
                              _sku = sku;
                              _selectedAttrs = selectedAttrsFromSku(sku);
                            }),
                          ),
                      ],
                    ),
                  if (_sku != null)
                    Text('已选 ${formatSkuAttributes(_sku!.attributes)} · 库存 ${_sku!.availableStock}'),
                ],
                const SizedBox(height: 12),
                Row(
                  children: [
                    const Text('数量'),
                    IconButton(
                      onPressed: _quantity > 1
                          ? () => setState(() => _quantity -= 1)
                          : null,
                      icon: const Icon(Icons.remove),
                    ),
                    Text('$_quantity'),
                    IconButton(
                      onPressed: () => setState(() => _quantity += 1),
                      icon: const Icon(Icons.add),
                    ),
                  ],
                ),
                FilledButton(
                  onPressed: _adding || _sku == null ? null : _addToCart,
                  child: Text(_adding ? '加入中…' : '加入购物车'),
                ),
                if (detail?.detail?.isNotEmpty == true) ...[
                  const SizedBox(height: 16),
                  Text(detail!.detail!),
                ],
                const SizedBox(height: 24),
                Text('评价', style: Theme.of(context).textTheme.titleMedium),
                if (_reviews.isEmpty)
                  const Padding(
                    padding: EdgeInsets.only(top: 8),
                    child: Text('暂无评价'),
                  )
                else
                  for (final review in _reviews)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${review.anonymous ? '匿名用户' : '用户'}  ${review.rating} 星'),
                      subtitle: Text(review.content ?? ''),
                    ),
              ],
            ),
    );
  }
}
