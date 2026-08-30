import 'package:dio/dio.dart';
import 'package:file_picker/file_picker.dart';
import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_exception.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/product/domain/product_models.dart';

class ProductApi {
  ProductApi(this._client);

  final ApiClient _client;

  Future<List<CategoryNode>> categoryTree() async {
    final response = await _client.get('/api/categories/tree');
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, CategoryNode.fromJson, message: '类目响应格式错误'),
    );
  }

  Future<PageResult<SearchItem>> search({
    String? keyword,
    String? categoryId,
    String sort = 'DEFAULT',
    int page = 1,
    int pageSize = 12,
  }) async {
    final response = await _client.get(
      '/api/search',
      queryParameters: {
        if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
        if (categoryId != null && categoryId.isNotEmpty) 'categoryId': categoryId,
        'sort': sort,
        'page': page,
        'pageSize': pageSize,
      },
    );
    return unwrapApiResponse(
      response.data,
      (value) => PageResult.fromJson(value, SearchItem.fromJson),
    );
  }

  Future<List<HotWord>> hotWords({int limit = 8}) async {
    final response = await _client.get(
      '/api/search/hot-words',
      queryParameters: {'limit': limit},
    );
    return unwrapApiResponse(response.data, (value) {
      final json = requireJsonMap(value);
      return readObjectList(json['words'], HotWord.fromJson);
    });
  }

  Future<SpuDetail> spuDetail(String spuId) async {
    final response = await _client.get('/api/spu/$spuId');
    return unwrapApiResponse(response.data, SpuDetail.fromJson);
  }

  Future<PageResult<Review>> reviews(String spuId, {int page = 1, int pageSize = 20}) async {
    final response = await _client.get(
      '/api/review/spu/$spuId',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return unwrapApiResponse(
      response.data,
      (value) => PageResult.fromJson(value, Review.fromJson),
    );
  }

  Future<ReviewStats> reviewStats(String spuId) async {
    final response = await _client.get('/api/review/spu/$spuId/stats');
    return unwrapApiResponse(response.data, ReviewStats.fromJson);
  }

  Future<PageResult<SpuItem>> merchantPage({
    String? keyword,
    String? shelf,
    int page = 1,
    int pageSize = 20,
  }) async {
    final response = await _client.get(
      '/api/merchant/spu/page',
      queryParameters: {
        if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
        if (shelf != null && shelf.isNotEmpty) 'shelf': shelf,
        'page': page,
        'pageSize': pageSize,
      },
    );
    return unwrapApiResponse(
      response.data,
      (value) => PageResult.fromJson(value, SpuItem.fromJson),
    );
  }

  Future<SpuDetail> merchantDetail(String spuId) async {
    final response = await _client.get('/api/merchant/spu/$spuId');
    return unwrapApiResponse(response.data, SpuDetail.fromJson);
  }

  Future<SpuDetail> createSpu({
    required String categoryId,
    required String name,
    String? brand,
    String? mainImage,
    required List<Map<String, Object?>> skus,
  }) async {
    final response = await _client.post(
      '/api/merchant/spu',
      data: {
        'categoryId': categoryId,
        'name': name,
        if (brand != null && brand.isNotEmpty) 'brand': brand,
        if (mainImage != null && mainImage.isNotEmpty) 'mainImage': mainImage,
        'skus': skus,
      },
    );
    return unwrapApiResponse(response.data, SpuDetail.fromJson);
  }

  Future<void> changeStatus(String spuId, String action, {String? remark}) async {
    final response = await _client.put(
      '/api/merchant/spu/$spuId/status',
      data: {'action': action, if (remark != null) 'remark': remark},
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> adjustStock(String skuId, int change, {String? remark}) async {
    final response = await _client.put(
      '/api/merchant/sku/$skuId/stock',
      data: {'change': change, if (remark != null) 'remark': remark},
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<ProductMedia> uploadMedia(PlatformFile file) async {
    final path = file.path;
    if (path == null || path.isEmpty) {
      throw const ApiException(message: '无法读取所选图片');
    }
    final form = FormData.fromMap({
      'file': await MultipartFile.fromFile(path, filename: file.name),
    });
    final response = await _client.postMultipart('/api/merchant/product-media', form);
    return unwrapApiResponse(response.data, ProductMedia.fromJson);
  }
}
