import 'package:shopping_app/core/network/api_response.dart';

class CartItem {
  const CartItem({
    required this.itemId,
    required this.skuId,
    required this.price,
    required this.quantity,
    required this.checked,
    required this.groupId,
    this.skuName,
    this.skuImage,
  });

  factory CartItem.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return CartItem(
      itemId: json['itemId'].toString(),
      skuId: json['skuId'].toString(),
      skuName: json['skuName'] as String?,
      skuImage: json['skuImage'] as String?,
      price: readDouble(json['price']),
      quantity: readInt(json['quantity'], fallback: 1),
      checked: readInt(json['checked']),
      groupId: json['groupId'].toString(),
    );
  }

  final String itemId;
  final String skuId;
  final String? skuName;
  final String? skuImage;
  final double price;
  final int quantity;
  final int checked;
  final String groupId;
}

class CartGroup {
  const CartGroup({
    required this.groupId,
    required this.shopId,
    required this.items,
    this.shopName,
  });

  factory CartGroup.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return CartGroup(
      groupId: json['groupId'].toString(),
      shopId: json['shopId'].toString(),
      shopName: json['shopName'] as String?,
      items: readObjectList(json['items'], CartItem.fromJson),
    );
  }

  final String groupId;
  final String shopId;
  final String? shopName;
  final List<CartItem> items;
}

class Cart {
  const Cart({required this.cartId, required this.groups});

  factory Cart.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return Cart(
      cartId: json['cartId'].toString(),
      groups: readObjectList(json['groups'], CartGroup.fromJson),
    );
  }

  final String cartId;
  final List<CartGroup> groups;
}

class AddCartItemRequest {
  const AddCartItemRequest({
    required this.skuId,
    required this.quantity,
    required this.shopId,
    required this.price,
  });

  final String skuId;
  final int quantity;
  final String shopId;
  final double price;

  Map<String, Object?> toJson() => {
    'skuId': skuId,
    'quantity': quantity,
    'shopId': shopId,
    'price': price,
  };
}

class OrderItem {
  const OrderItem({
    required this.itemId,
    required this.skuId,
    required this.price,
    required this.quantity,
    required this.status,
    required this.totalAmount,
    this.skuName,
    this.skuImage,
  });

  factory OrderItem.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return OrderItem(
      itemId: json['itemId'].toString(),
      skuId: json['skuId'].toString(),
      skuName: json['skuName'] as String?,
      skuImage: json['skuImage'] as String?,
      price: readDouble(json['price']),
      quantity: readInt(json['quantity']),
      status: readInt(json['status']),
      totalAmount: readDouble(json['totalAmount']),
    );
  }

  final String itemId;
  final String skuId;
  final String? skuName;
  final String? skuImage;
  final double price;
  final int quantity;
  final int status;
  final double totalAmount;
}

class Order {
  const Order({
    required this.orderId,
    required this.orderNo,
    required this.status,
    required this.totalAmount,
    required this.discountAmount,
    required this.freightAmount,
    required this.payAmount,
    required this.receiverName,
    required this.receiverPhone,
    required this.receiverAddress,
    required this.items,
    this.remark,
    this.closeTime,
    this.payTime,
    this.finishTime,
  });

  factory Order.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return Order(
      orderId: json['orderId'].toString(),
      orderNo: json['orderNo']?.toString() ?? '',
      status: readInt(json['status']),
      totalAmount: readDouble(json['totalAmount']),
      discountAmount: readDouble(json['discountAmount']),
      freightAmount: readDouble(json['freightAmount']),
      payAmount: readDouble(json['payAmount']),
      receiverName: json['receiverName']?.toString() ?? '',
      receiverPhone: json['receiverPhone']?.toString() ?? '',
      receiverAddress: json['receiverAddress']?.toString() ?? '',
      remark: json['remark'] as String?,
      closeTime: json['closeTime'] as String?,
      payTime: json['payTime'] as String?,
      finishTime: json['finishTime'] as String?,
      items: readObjectList(json['items'], OrderItem.fromJson),
    );
  }

  final String orderId;
  final String orderNo;
  final int status;
  final double totalAmount;
  final double discountAmount;
  final double freightAmount;
  final double payAmount;
  final String receiverName;
  final String receiverPhone;
  final String receiverAddress;
  final String? remark;
  final String? closeTime;
  final String? payTime;
  final String? finishTime;
  final List<OrderItem> items;
}

class PaymentOrder {
  const PaymentOrder({
    required this.paymentOrderId,
    required this.paymentNo,
    required this.orderId,
    required this.userId,
    required this.amount,
    required this.payChannel,
    required this.status,
    this.payTime,
    this.expireTime,
  });

  factory PaymentOrder.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return PaymentOrder(
      paymentOrderId: json['paymentOrderId'].toString(),
      paymentNo: json['paymentNo']?.toString() ?? '',
      orderId: json['orderId'].toString(),
      userId: json['userId'].toString(),
      amount: readDouble(json['amount']),
      payChannel: json['payChannel']?.toString() ?? '',
      status: readInt(json['status']),
      payTime: json['payTime'] as String?,
      expireTime: json['expireTime'] as String?,
    );
  }

  final String paymentOrderId;
  final String paymentNo;
  final String orderId;
  final String userId;
  final double amount;
  final String payChannel;
  final int status;
  final String? payTime;
  final String? expireTime;
}

const orderStatusLabels = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已取消',
  5: '已关闭',
  6: '退款中',
  7: '退款完成',
};

const paymentStatusLabels = {0: '待支付', 1: '已支付'};
