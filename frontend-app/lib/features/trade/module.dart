import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module.dart';
import 'package:shopping_app/features/trade/presentation/cart_page.dart';
import 'package:shopping_app/features/trade/presentation/checkout_page.dart';
import 'package:shopping_app/features/trade/presentation/order_detail_page.dart';
import 'package:shopping_app/features/trade/presentation/order_list_page.dart';
import 'package:shopping_app/features/trade/presentation/payment_page.dart';

final tradeModule = AppModuleContribution(
  key: 'trade',
  owner: '成员 4',
  routes: [
    GoRoute(path: '/cart', builder: (context, state) => const CartPage()),
    GoRoute(
      path: '/checkout',
      builder: (context, state) => const CheckoutPage(),
    ),
    GoRoute(path: '/orders', builder: (context, state) => const OrderListPage()),
    GoRoute(
      path: '/orders/:orderId',
      builder: (context, state) =>
          OrderDetailPage(orderId: state.pathParameters['orderId']!),
    ),
    GoRoute(
      path: '/pay/:paymentOrderId',
      builder: (context, state) => PaymentPage(
        paymentOrderId: state.pathParameters['paymentOrderId']!,
      ),
    ),
  ],
);
