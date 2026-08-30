import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';

const _presets = [
  ('颜色', '黑色,白色,原色'),
  ('内存', '128GB,256GB,512GB'),
  ('尺码', 'S,M,L,XL'),
  ('口味', '原味,微辣,麻辣'),
];

class MerchantAddProductPage extends ConsumerStatefulWidget {
  const MerchantAddProductPage({super.key});

  @override
  ConsumerState<MerchantAddProductPage> createState() =>
      _MerchantAddProductPageState();
}

class _MerchantAddProductPageState extends ConsumerState<MerchantAddProductPage> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _brandController = TextEditingController();
  final _basePriceController = TextEditingController(text: '99.9');
  final _baseStockController = TextEditingController(text: '100');
  List<CategoryNode> _categories = const [];
  String? _categoryId;
  String? _mainImage;
  List<SpecDimension> _dimensions = [
    createDimension('颜色', '黑色,白色,原色'),
    createDimension('内存', '128GB,256GB,512GB'),
  ];
  List<SkuDraft> _skus = [];
  String _hint = '填写规格属性后点「按属性组合生成 SKU」，再核对每个规格的价格和库存。';
  bool _loading = true;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _skus = [emptySkuDraft(_dimensions)];
    Future<void>.microtask(_loadCategories);
  }

  @override
  void dispose() {
    _nameController.dispose();
    _brandController.dispose();
    _basePriceController.dispose();
    _baseStockController.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    try {
      final tree = await ref.read(productApiProvider).categoryTree();
      final flat = flattenCategories(tree);
      if (mounted) {
        setState(() {
          _categories = flat;
          _categoryId = flat.isEmpty ? null : flat.first.id;
          _loading = false;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _loading = false);
        _toast(error.toString());
      }
    }
  }

  Future<void> _pickImage() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.image,
      allowMultiple: false,
    );
    final file = (result == null || result.files.isEmpty) ? null : result.files.first;
    if (file == null) return;
    try {
      final media = await ref.read(productApiProvider).uploadMedia(file);
      if (mounted) setState(() => _mainImage = media.url);
    } catch (error) {
      _toast(error.toString());
    }
  }

  void _addPreset((String, String) preset) {
    setState(() {
      final existing = _dimensions.where((dim) => dim.name.trim() == preset.$1);
      if (existing.isNotEmpty) {
        final dim = existing.first;
        final current = splitSpecValues(dim.values);
        for (final value in splitSpecValues(preset.$2)) {
          if (!current.contains(value)) current.add(value);
        }
        dim.values = current.join(',');
        return;
      }
      _dimensions = [..._dimensions, createDimension(preset.$1, preset.$2)];
    });
  }

  void _generate() {
    final result = generateSkuDrafts(
      _dimensions,
      _basePriceController.text.trim(),
      _baseStockController.text.trim(),
    );
    if (result.error != null || result.skus == null) {
      _toast(result.error ?? '无法生成 SKU');
      return;
    }
    setState(() {
      _skus = result.skus!;
      _hint = result.summary ?? _hint;
    });
  }

  List<Map<String, Object?>>? _payload() {
    final used = <String>{};
    final skus = <Map<String, Object?>>[];
    for (final draft in _skus) {
      final price = double.tryParse(draft.price);
      final stock = int.tryParse(draft.stock);
      if (price == null || price < 0.01) {
        _toast('每个 SKU 的价格须大于 0');
        return null;
      }
      if (stock == null || stock < 0) {
        _toast('每个 SKU 的库存须为不小于 0 的整数');
        return null;
      }
      final attributes = skuAttributesJson(_dimensions, draft.attrs);
      if (attributes == null) {
        _toast('每个 SKU 请至少填写一个规格属性取值，可先点「按属性组合生成 SKU」');
        return null;
      }
      skus.add({
        'skuCode': uniqueSkuCode(
          draft.skuCode.trim().isEmpty
              ? suggestSkuCode(_dimensions, draft.attrs)
              : draft.skuCode.trim(),
          used,
        ),
        'attributes': attributes,
        'price': price,
        'stock': stock,
      });
    }
    return skus;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || _categoryId == null) return;
    final skus = _payload();
    if (skus == null) return;
    setState(() => _submitting = true);
    try {
      final created = await ref.read(productApiProvider).createSpu(
        categoryId: _categoryId!,
        name: _nameController.text.trim(),
        brand: _brandController.text.trim(),
        mainImage: _mainImage,
        skus: skus,
      );
      await ref.read(productApiProvider).changeStatus(created.id, 'SUBMIT');
      if (mounted) {
        _toast('商品已提交审核');
        context.go('/merchant/products');
      }
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _toast(String text) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('添加商品')),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                DropdownButtonFormField<String>(
                  value: _categoryId,
                  decoration: const InputDecoration(labelText: '类目'),
                  items: [
                    for (final category in _categories)
                      DropdownMenuItem(value: category.id, child: Text(category.name)),
                  ],
                  onChanged: (value) => setState(() => _categoryId = value),
                  validator: (value) =>
                      value == null || value.isEmpty ? '请选择类目' : null,
                ),
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: '商品名称'),
                  validator: (value) =>
                      value == null || value.trim().isEmpty ? '请输入商品名称' : null,
                ),
                TextFormField(
                  controller: _brandController,
                  decoration: const InputDecoration(labelText: '品牌（选填）'),
                ),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: _submitting ? null : _pickImage,
                  icon: const Icon(Icons.image_outlined),
                  label: Text(_mainImage == null ? '上传主图（可选）' : '主图已上传'),
                ),
                const SizedBox(height: 20),
                Text('规格属性', style: Theme.of(context).textTheme.titleMedium),
                const Text('与用户 Web 相同：自定义颜色、内存、尺码等，再按取值组合生成 SKU。'),
                Wrap(
                  spacing: 8,
                  children: [
                    for (final preset in _presets)
                      ActionChip(
                        label: Text('+ ${preset.$1}'),
                        onPressed: () => _addPreset(preset),
                      ),
                  ],
                ),
                for (var i = 0; i < _dimensions.length; i++)
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          initialValue: _dimensions[i].name,
                          decoration: const InputDecoration(labelText: '属性名'),
                          onChanged: (value) => _dimensions[i].name = value,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        flex: 2,
                        child: TextFormField(
                          initialValue: _dimensions[i].values,
                          decoration: const InputDecoration(labelText: '取值，逗号分隔'),
                          onChanged: (value) => _dimensions[i].values = value,
                        ),
                      ),
                      IconButton(
                        onPressed: _dimensions.length <= 1
                            ? null
                            : () => setState(() {
                                _dimensions = [
                                  for (var j = 0; j < _dimensions.length; j++)
                                    if (j != i) _dimensions[j],
                                ];
                              }),
                        icon: const Icon(Icons.remove_circle_outline),
                      ),
                    ],
                  ),
                TextButton.icon(
                  onPressed: () =>
                      setState(() => _dimensions = [..._dimensions, createDimension()]),
                  icon: const Icon(Icons.add),
                  label: const Text('添加规格属性'),
                ),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _basePriceController,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: '起步价'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextFormField(
                        controller: _baseStockController,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(labelText: '各规格库存'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                FilledButton.tonal(
                  onPressed: _generate,
                  child: const Text('按属性组合生成 SKU'),
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 8, bottom: 12),
                  child: Text(_hint),
                ),
                Text('SKU 与库存', style: Theme.of(context).textTheme.titleMedium),
                for (var i = 0; i < _skus.length; i++)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(_skus[i].attrText),
                          TextFormField(
                            initialValue: _skus[i].skuCode,
                            decoration: const InputDecoration(labelText: 'SKU 编码'),
                            onChanged: (value) => _skus[i].skuCode = value,
                          ),
                          Row(
                            children: [
                              Expanded(
                                child: TextFormField(
                                  initialValue: _skus[i].price,
                                  decoration: const InputDecoration(labelText: '价格'),
                                  onChanged: (value) => _skus[i].price = value,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: TextFormField(
                                  initialValue: _skus[i].stock,
                                  decoration: const InputDecoration(labelText: '库存'),
                                  onChanged: (value) => _skus[i].stock = value,
                                ),
                              ),
                              IconButton(
                                onPressed: _skus.length <= 1
                                    ? null
                                    : () => setState(() {
                                        _skus = [
                                          for (var j = 0; j < _skus.length; j++)
                                            if (j != i) _skus[j],
                                        ];
                                      }),
                                icon: const Icon(Icons.remove_circle_outline),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                TextButton.icon(
                  onPressed: () => setState(() {
                    _skus = [
                      ..._skus,
                      emptySkuDraft(
                        _dimensions,
                        price: _basePriceController.text,
                        stock: _baseStockController.text,
                      ),
                    ];
                  }),
                  icon: const Icon(Icons.add),
                  label: const Text('添加 SKU'),
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _submitting ? null : _submit,
                  child: Text(_submitting ? '提交中…' : '创建并提交审核'),
                ),
              ],
            ),
          ),
  );
}
