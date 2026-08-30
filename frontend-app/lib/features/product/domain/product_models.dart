import 'dart:convert';

import 'package:shopping_app/core/network/api_response.dart';

class CategoryNode {
  const CategoryNode({
    required this.id,
    required this.parentId,
    required this.name,
    required this.level,
    required this.sortOrder,
    required this.status,
    required this.children,
    this.icon,
  });

  factory CategoryNode.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return CategoryNode(
      id: json['id'].toString(),
      parentId: json['parentId'].toString(),
      name: json['name']?.toString() ?? '',
      level: readInt(json['level']),
      sortOrder: readInt(json['sortOrder']),
      icon: json['icon'] as String?,
      status: readInt(json['status']),
      children: readObjectList(json['children'], CategoryNode.fromJson),
    );
  }

  final String id;
  final String parentId;
  final String name;
  final int level;
  final int sortOrder;
  final String? icon;
  final int status;
  final List<CategoryNode> children;
}

List<CategoryNode> flattenCategories(List<CategoryNode> tree) {
  final result = <CategoryNode>[];
  void walk(List<CategoryNode> nodes) {
    for (final node in nodes) {
      result.add(node);
      walk(node.children);
    }
  }

  walk(tree);
  return result;
}

class Sku {
  const Sku({
    required this.id,
    required this.spuId,
    required this.price,
    required this.availableStock,
    required this.reservedStock,
    required this.soldStock,
    required this.status,
    this.skuCode,
    this.attributes,
    this.image,
  });

  factory Sku.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return Sku(
      id: json['id'].toString(),
      spuId: json['spuId'].toString(),
      skuCode: json['skuCode'] as String?,
      attributes: readSkuAttributes(json['attributes']),
      image: json['image'] as String?,
      price: readDouble(json['price']),
      availableStock: readInt(json['availableStock']),
      reservedStock: readInt(json['reservedStock']),
      soldStock: readInt(json['soldStock']),
      status: readInt(json['status']),
    );
  }

  final String id;
  final String spuId;
  final String? skuCode;
  final String? attributes;
  final String? image;
  final double price;
  final int availableStock;
  final int reservedStock;
  final int soldStock;
  final int status;
}

class SpuItem {
  const SpuItem({
    required this.id,
    required this.shopId,
    required this.categoryId,
    required this.name,
    required this.sales,
    required this.rating,
    required this.status,
    this.shopName,
    this.brand,
    this.subtitle,
    this.mainImage,
    this.priceMin,
    this.priceMax,
  });

  factory SpuItem.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return SpuItem(
      id: json['id'].toString(),
      shopId: json['shopId'].toString(),
      shopName: json['shopName'] as String?,
      categoryId: json['categoryId'].toString(),
      brand: json['brand'] as String?,
      name: json['name']?.toString() ?? '',
      subtitle: json['subtitle'] as String?,
      mainImage: json['mainImage'] as String?,
      priceMin: json['priceMin'] == null ? null : readDouble(json['priceMin']),
      priceMax: json['priceMax'] == null ? null : readDouble(json['priceMax']),
      sales: readInt(json['sales']),
      rating: readDouble(json['rating']),
      status: json['status']?.toString() ?? '',
    );
  }

  final String id;
  final String shopId;
  final String? shopName;
  final String categoryId;
  final String? brand;
  final String name;
  final String? subtitle;
  final String? mainImage;
  final double? priceMin;
  final double? priceMax;
  final int sales;
  final double rating;
  final String status;
}

class SpuDetail {
  const SpuDetail({
    required this.id,
    required this.shopId,
    required this.categoryId,
    required this.name,
    required this.sales,
    required this.rating,
    required this.status,
    required this.createdAt,
    required this.images,
    required this.skus,
    this.shopName,
    this.brand,
    this.subtitle,
    this.mainImage,
    this.detail,
    this.priceMin,
    this.priceMax,
    this.auditRemark,
  });

  factory SpuDetail.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return SpuDetail(
      id: json['id'].toString(),
      shopId: json['shopId'].toString(),
      shopName: json['shopName'] as String?,
      categoryId: json['categoryId'].toString(),
      brand: json['brand'] as String?,
      name: json['name']?.toString() ?? '',
      subtitle: json['subtitle'] as String?,
      mainImage: json['mainImage'] as String?,
      images: readStringList(json['images']),
      detail: json['detail'] as String?,
      priceMin: json['priceMin'] == null ? null : readDouble(json['priceMin']),
      priceMax: json['priceMax'] == null ? null : readDouble(json['priceMax']),
      sales: readInt(json['sales']),
      rating: readDouble(json['rating']),
      status: json['status']?.toString() ?? '',
      auditRemark: json['auditRemark'] as String?,
      createdAt: json['createdAt']?.toString() ?? '',
      skus: readObjectList(json['skus'], Sku.fromJson),
    );
  }

  final String id;
  final String shopId;
  final String? shopName;
  final String categoryId;
  final String? brand;
  final String name;
  final String? subtitle;
  final String? mainImage;
  final List<String> images;
  final String? detail;
  final double? priceMin;
  final double? priceMax;
  final int sales;
  final double rating;
  final String status;
  final String? auditRemark;
  final String createdAt;
  final List<Sku> skus;
}

class SearchItem {
  const SearchItem({
    required this.spuId,
    required this.shopId,
    required this.categoryId,
    required this.name,
    required this.sales,
    required this.rating,
    this.brand,
    this.subtitle,
    this.mainImage,
    this.priceMin,
    this.priceMax,
  });

  factory SearchItem.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return SearchItem(
      spuId: json['spuId'].toString(),
      shopId: json['shopId'].toString(),
      categoryId: json['categoryId'].toString(),
      brand: json['brand'] as String?,
      name: json['name']?.toString() ?? '',
      subtitle: json['subtitle'] as String?,
      mainImage: json['mainImage'] as String?,
      priceMin: json['priceMin'] == null ? null : readDouble(json['priceMin']),
      priceMax: json['priceMax'] == null ? null : readDouble(json['priceMax']),
      sales: readInt(json['sales']),
      rating: readDouble(json['rating']),
    );
  }

  final String spuId;
  final String shopId;
  final String categoryId;
  final String? brand;
  final String name;
  final String? subtitle;
  final String? mainImage;
  final double? priceMin;
  final double? priceMax;
  final int sales;
  final double rating;
}

class PageResult<T> {
  const PageResult({
    required this.items,
    required this.total,
    required this.page,
    required this.pageSize,
  });

  factory PageResult.fromJson(
    Object? value,
    T Function(Object? item) parseItem,
  ) {
    final json = requireJsonMap(value);
    return PageResult(
      items: readObjectList(json['items'], parseItem),
      total: json['total']?.toString() ?? '0',
      page: readInt(json['page'], fallback: 1),
      pageSize: readInt(json['pageSize'], fallback: 20),
    );
  }

  final List<T> items;
  final String total;
  final int page;
  final int pageSize;
}

class HotWord {
  const HotWord({required this.keyword, required this.count});

  factory HotWord.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return HotWord(
      keyword: json['keyword']?.toString() ?? '',
      count: json['count']?.toString() ?? '0',
    );
  }

  final String keyword;
  final String count;
}

class Review {
  const Review({
    required this.id,
    required this.spuId,
    required this.skuId,
    required this.userId,
    required this.rating,
    required this.anonymous,
    required this.createdAt,
    required this.images,
    this.content,
    this.reply,
  });

  factory Review.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return Review(
      id: json['id'].toString(),
      spuId: json['spuId'].toString(),
      skuId: json['skuId'].toString(),
      userId: json['userId'].toString(),
      rating: readInt(json['rating']),
      content: json['content'] as String?,
      images: readStringList(json['images']),
      anonymous: json['anonymous'] == true,
      createdAt: json['createdAt']?.toString() ?? '',
      reply: json['reply'] as String?,
    );
  }

  final String id;
  final String spuId;
  final String skuId;
  final String userId;
  final int rating;
  final String? content;
  final List<String> images;
  final bool anonymous;
  final String createdAt;
  final String? reply;
}

class ReviewStats {
  const ReviewStats({
    required this.averageRating,
    required this.totalCount,
    required this.fiveStar,
    required this.fourStar,
    required this.threeStar,
    required this.twoStar,
    required this.oneStar,
    required this.positiveRate,
  });

  factory ReviewStats.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ReviewStats(
      averageRating: readDouble(json['averageRating']),
      totalCount: json['totalCount']?.toString() ?? '0',
      fiveStar: json['fiveStar']?.toString() ?? '0',
      fourStar: json['fourStar']?.toString() ?? '0',
      threeStar: json['threeStar']?.toString() ?? '0',
      twoStar: json['twoStar']?.toString() ?? '0',
      oneStar: json['oneStar']?.toString() ?? '0',
      positiveRate: readDouble(json['positiveRate']),
    );
  }

  final double averageRating;
  final String totalCount;
  final String fiveStar;
  final String fourStar;
  final String threeStar;
  final String twoStar;
  final String oneStar;
  final double positiveRate;
}

class ProductMedia {
  const ProductMedia({
    required this.id,
    required this.url,
    required this.contentType,
  });

  factory ProductMedia.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ProductMedia(
      id: json['id'].toString(),
      url: json['url']?.toString() ?? '',
      contentType: json['contentType']?.toString() ?? '',
    );
  }

  final String id;
  final String url;
  final String contentType;
}

const spuStatusLabels = {
  'DRAFT': '草稿',
  'PENDING_AUDIT': '待审核',
  'AUDIT_APPROVED': '审核通过',
  'AUDIT_REJECTED': '已驳回/已收回',
  'ON_SALE': '上架中',
  'OFF_SALE': '已下架',
};

String formatMoney(double? value) =>
    value == null ? '—' : '¥${value.toStringAsFixed(2)}';

String? readSkuAttributes(Object? value) {
  if (value == null) return null;
  if (value is String) return value;
  if (value is Map) return jsonEncode(value);
  return value.toString();
}

String formatSkuAttributes(String? raw, [String empty = '默认规格']) {
  if (raw == null || raw.trim().isEmpty) return empty;
  try {
    final parsed = jsonDecode(raw);
    if (parsed is Map) {
      final parts = parsed.entries
          .where((entry) => '${entry.value}'.trim().isNotEmpty)
          .map((entry) => '${entry.key}：${entry.value}')
          .toList();
      if (parts.isNotEmpty) return parts.join(' / ');
    }
  } catch (_) {}
  return raw;
}

Map<String, String> parseSkuAttributes(String? raw) {
  if (raw == null || raw.trim().isEmpty) return {};
  try {
    final parsed = jsonDecode(raw);
    if (parsed is Map) {
      return {
        for (final entry in parsed.entries)
          if ('${entry.value}'.trim().isNotEmpty) '${entry.key}': '${entry.value}',
      };
    }
  } catch (_) {}
  return {'规格': raw};
}

class SpecDimension {
  SpecDimension({required this.id, this.name = '', this.values = ''});

  final String id;
  String name;
  String values;
}

class SkuDraft {
  SkuDraft({
    required this.key,
    this.skuCode = '',
    Map<String, String>? attrs,
    this.attrText = '待填写规格',
    this.price = '99.9',
    this.stock = '100',
  }) : attrs = attrs ?? {};

  final String key;
  String skuCode;
  final Map<String, String> attrs;
  String attrText;
  String price;
  String stock;
}

List<String> splitSpecValues(String raw) => raw
    .split(RegExp('[,，、;；]+'))
    .map((item) => item.trim())
    .where((item) => item.isNotEmpty)
    .toList();

int _dimSeq = 1;
int _skuSeq = 1;

SpecDimension createDimension([String name = '', String values = '']) =>
    SpecDimension(id: 'dim-${_dimSeq++}', name: name, values: values);

SkuDraft emptySkuDraft(List<SpecDimension> dimensions, {String price = '99.9', String stock = '100'}) =>
    SkuDraft(
      key: 'sku-${_skuSeq++}',
      attrs: {for (final dim in dimensions) dim.id: ''},
      price: price,
      stock: stock,
    );

const _codeMap = {
  '黑色': 'BLK',
  '白色': 'WHT',
  '原色': 'GLD',
  '蓝色': 'BLU',
  '标准版': 'STD',
  '豪华版': 'PRO',
};

String _codePart(String value) {
  final compact = value.replaceAll(RegExp(r'\s+'), '');
  return _codeMap[compact] ?? compact.replaceAll(RegExp('GB', caseSensitive: false), '').substring(0, compact.length > 6 ? 6 : compact.length);
}

String suggestSkuCode(List<SpecDimension> dimensions, Map<String, String> attrs) => dimensions
    .map((dim) => attrs[dim.id]?.trim() ?? '')
    .where((value) => value.isNotEmpty)
    .map(_codePart)
    .join('-');

String uniqueSkuCode(String base, Set<String> used) {
  final seed = base.trim().isEmpty ? 'SKU' : base.trim();
  var code = seed;
  var serial = 2;
  while (used.contains(code.toLowerCase())) {
    code = '$seed-${serial++}';
  }
  used.add(code.toLowerCase());
  return code;
}

List<List<String>> _cartesian(List<List<String>> lists) {
  var acc = <List<String>>[];
  for (final list in lists) {
    if (list.isEmpty) return [];
    if (acc.isEmpty) {
      acc = [
        for (final value in list) [value],
      ];
      continue;
    }
    acc = [
      for (final prefix in acc)
        for (final value in list) [...prefix, value],
    ];
  }
  return acc;
}

double _priceForCombo(List<SpecDimension> dimensions, Map<String, String> combo, double start) {
  for (final dim in dimensions) {
    if (!RegExp(r'内存|容量|存储').hasMatch(dim.name)) continue;
    final gb = int.tryParse(RegExp(r'\d+').stringMatch(combo[dim.id] ?? '') ?? '');
    if (gb == null) continue;
    if (gb <= 128) return start;
    if (gb <= 256) return start + 800;
    if (gb <= 512) return start + 2000;
    return start + 3200;
  }
  return start;
}

String? skuAttributesJson(List<SpecDimension> dimensions, Map<String, String> attrs) {
  final attributes = <String, String>{};
  for (final dim in dimensions) {
    final name = dim.name.trim();
    final value = attrs[dim.id]?.trim() ?? '';
    if (name.isNotEmpty && value.isNotEmpty) attributes[name] = value;
  }
  return attributes.isEmpty ? null : jsonEncode(attributes);
}

({List<SkuDraft>? skus, String? error, String? summary}) generateSkuDrafts(
  List<SpecDimension> dimensions,
  String basePrice,
  String baseStock,
) {
  final named = dimensions.where((dim) => dim.name.trim().isNotEmpty).toList();
  if (named.isEmpty) {
    return (skus: null, error: '请先添加规格属性，例如「颜色」「内存」「尺码」', summary: null);
  }
  for (final dim in named) {
    if (splitSpecValues(dim.values).isEmpty) {
      return (skus: null, error: '请为「${dim.name}」填写取值，多个值用逗号分隔', summary: null);
    }
  }
  final start = double.tryParse(basePrice);
  final stock = int.tryParse(baseStock);
  if (start == null || start < 0.01 || stock == null || stock < 0) {
    return (skus: null, error: '生成规格前请填写有效的起步价和库存', summary: null);
  }
  final valueLists = [for (final dim in named) splitSpecValues(dim.values)];
  final combos = _cartesian(valueLists);
  if (combos.length > 80) {
    return (skus: null, error: '组合过多（${combos.length}），请减少属性取值后再生成', summary: null);
  }
  final used = <String>{};
  final skus = [
    for (var i = 0; i < combos.length; i++)
      SkuDraft(
        key: 'sku-${_skuSeq++}',
        skuCode: uniqueSkuCode(
          suggestSkuCode(named, {
            for (var j = 0; j < named.length; j++) named[j].id: combos[i][j],
          }),
          used,
        ),
        attrs: {
          for (var j = 0; j < named.length; j++) named[j].id: combos[i][j],
        },
        price: _priceForCombo(named, {
          for (var j = 0; j < named.length; j++) named[j].id: combos[i][j],
        }, start).toString(),
        stock: '$stock',
        attrText: formatSkuAttributes(
          skuAttributesJson(named, {
            for (var j = 0; j < named.length; j++) named[j].id: combos[i][j],
          }),
        ),
      ),
  ];
  final summary = named.asMap().entries.map((entry) => '${entry.value.name} ${valueLists[entry.key].length} 档').join(' × ');
  return (skus: skus, error: null, summary: '已生成 ${skus.length} 个 SKU（$summary）');
}

Map<String, String> selectedAttrsFromSku(Sku? sku) =>
    sku == null ? {} : parseSkuAttributes(sku.attributes);

Sku? matchSku(List<Sku> skus, Map<String, String> selected) {
  for (final sku in skus) {
    final attrs = parseSkuAttributes(sku.attributes);
    if (attrs.isEmpty && selected.isEmpty) return sku;
    if (attrs.keys.every((key) => selected[key] == null || selected[key] == attrs[key])) {
      return sku;
    }
  }
  return skus.isEmpty ? null : skus.first;
}

List<({String name, List<({String value, bool selected})> values})> specAxesFromSkus(
  List<Sku> skus,
  Map<String, String> selected,
) {
  final order = <String>[];
  final map = <String, List<String>>{};
  for (final sku in skus) {
    for (final entry in parseSkuAttributes(sku.attributes).entries) {
      map.putIfAbsent(entry.key, () {
        order.add(entry.key);
        return <String>[];
      });
      if (!map[entry.key]!.contains(entry.value)) map[entry.key]!.add(entry.value);
    }
  }
  return [
    for (final name in order)
      (
        name: name,
        values: [
          for (final value in map[name] ?? const <String>[])
            (value: value, selected: selected[name] == value),
        ],
      ),
  ];
}
