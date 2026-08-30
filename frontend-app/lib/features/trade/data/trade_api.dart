import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/trade/domain/trade_models.dart';

class TradeApi {
  TradeApi(this._client);

  final ApiClient _client;

  Future<Cart> getCart() async {
    final response = await _client.get('/api/cart');
    return unwrapApiResponse(response.data, Cart.fromJson);
  }

  Future<void> addCartItem(AddCartItemRequest request) async {
    final response = await _client.post(
      '/api/cart/items',
      data: request.toJson(),
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> updateQuantity(String itemId, int quantity) async {
    final response = await _client.put(
      '/api/cart/items/$itemId/quantity',
      data: {'quantity': quantity},
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> updateChecked(String itemId, bool checked) async {
    final response = await _client.put(
      '/api/cart/items/$itemId/checked',
      data: {'checked': checked ? 1 : 0},
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> removeItem(String itemId) async {
    final response = await _client.delete('/api/cart/items/$itemId');
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<List<Order>> createOrder({
    required String receiverName,
    required String receiverPhone,
    required String receiverAddress,
    String? remark,
  }) async {
    final response = await _client.post(
      '/api/orders',
      data: {
        'receiverName': receiverName,
        'receiverPhone': receiverPhone,
        'receiverAddress': receiverAddress,
        if (remark != null && remark.isNotEmpty) 'remark': remark,
      },
    );
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, Order.fromJson, message: '订单响应格式错误'),
    );
  }

  Future<List<Order>> listOrders() async {
    final response = await _client.get('/api/orders');
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, Order.fromJson, message: '订单列表格式错误'),
    );
  }

  Future<Order> orderDetail(String orderId) async {
    final response = await _client.get('/api/orders/$orderId');
    return unwrapApiResponse(response.data, Order.fromJson);
  }

  Future<void> cancelOrder(String orderId) async {
    final response = await _client.put('/api/orders/$orderId/cancel');
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> confirmOrder(String orderId) async {
    final response = await _client.put('/api/orders/$orderId/confirm');
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<PaymentOrder> createPayment(String orderId) async {
    final response = await _client.post(
      '/api/payments',
      data: {'orderId': orderId},
    );
    return unwrapApiResponse(response.data, PaymentOrder.fromJson);
  }

  Future<PaymentOrder> mockPay(String paymentOrderId) async {
    final response = await _client.post(
      '/api/payments/$paymentOrderId/mock-pay',
    );
    return unwrapApiResponse(response.data, PaymentOrder.fromJson);
  }

  Future<PaymentOrder> paymentDetail(String paymentOrderId) async {
    final response = await _client.get('/api/payments/$paymentOrderId');
    return unwrapApiResponse(response.data, PaymentOrder.fromJson);
  }

  Future<void> createRefund({
    required String orderId,
    double? amount,
    String? reason,
  }) async {
    final response = await _client.post(
      '/api/refunds',
      data: {
        'orderId': orderId,
        if (amount != null) 'amount': amount,
        if (reason != null && reason.isNotEmpty) 'reason': reason,
      },
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }
}
