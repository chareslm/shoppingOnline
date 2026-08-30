import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module.dart';
import 'package:shopping_app/features/product/presentation/merchant_add_product_page.dart';
import 'package:shopping_app/features/product/presentation/merchant_product_list_page.dart';
import 'package:shopping_app/features/product/presentation/product_detail_page.dart';
import 'package:shopping_app/features/product/presentation/product_list_page.dart';

final productModule = AppModuleContribution(
  key: 'product',
  owner: '成员 3',
  routes: [
    GoRoute(
      path: '/products',
      builder: (context, state) => const ProductListPage(),
    ),
    GoRoute(
      path: '/products/:spuId',
      builder: (context, state) =>
          ProductDetailPage(spuId: state.pathParameters['spuId']!),
    ),
    GoRoute(
      path: '/merchant/products',
      builder: (context, state) => const MerchantProductListPage(),
    ),
    GoRoute(
      path: '/merchant/add-product',
      builder: (context, state) => const MerchantAddProductPage(),
    ),
  ],
);
