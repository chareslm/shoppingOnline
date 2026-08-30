import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/config/app_config.dart';
import 'package:shopping_app/core/media/media_url.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';

class ProductListPage extends ConsumerStatefulWidget {
  const ProductListPage({super.key});

  @override
  ConsumerState<ProductListPage> createState() => _ProductListPageState();
}

class _ProductListPageState extends ConsumerState<ProductListPage> {
  final _keywordController = TextEditingController();
  List<SearchItem> _items = const [];
  List<HotWord> _hotWords = const [];
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_bootstrap);
  }

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  Future<void> _bootstrap() async {
    try {
      final words = await ref.read(productApiProvider).hotWords();
      if (mounted) setState(() => _hotWords = words);
    } catch (_) {}
    await _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await ref
          .read(productApiProvider)
          .search(keyword: _keywordController.text.trim());
      if (mounted) setState(() => _items = result.items);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('商品')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _keywordController,
            textInputAction: TextInputAction.search,
            onSubmitted: (_) => _load(),
            decoration: InputDecoration(
              labelText: '搜索商品',
              suffixIcon: IconButton(
                onPressed: _load,
                icon: const Icon(Icons.search),
              ),
            ),
          ),
          if (_hotWords.isNotEmpty) ...[
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [
                for (final word in _hotWords)
                  ActionChip(
                    label: Text(word.keyword),
                    onPressed: () {
                      _keywordController.text = word.keyword;
                      _load();
                    },
                  ),
              ],
            ),
          ],
          if (_error != null) ...[
            const SizedBox(height: 16),
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
          if (_loading && _items.isEmpty)
            const Padding(
              padding: EdgeInsets.only(top: 48),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_items.isEmpty && !_loading)
            const Padding(
              padding: EdgeInsets.only(top: 48),
              child: Center(child: Text('暂无商品')),
            )
          else
            for (final item in _items)
              Card(
                child: ListTile(
                  leading: _thumb(item.mainImage),
                  title: Text(item.name),
                  subtitle: Text(
                    '${formatMoney(item.priceMin)}  · 销量 ${item.sales}',
                  ),
                  onTap: () => context.push('/products/${item.spuId}'),
                ),
              ),
        ],
      ),
    ),
  );
}

Widget _thumb(String? path) {
  final url = mediaUrl(AppConfig.apiBaseUrl, path);
  if (url.isEmpty) return const Icon(Icons.image_not_supported_outlined);
  return Image.network(
    url,
    width: 48,
    height: 48,
    fit: BoxFit.cover,
    errorBuilder: (_, _, _) => const Icon(Icons.broken_image_outlined),
  );
}
