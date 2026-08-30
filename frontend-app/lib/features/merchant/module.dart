import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module.dart';
import 'package:shopping_app/features/merchant/presentation/merchant_home_page.dart';
import 'package:shopping_app/features/merchant/presentation/merchant_staff_page.dart';
import 'package:shopping_app/features/merchant/presentation/merchant_stats_page.dart';

final merchantModule = AppModuleContribution(
  key: 'merchant',
  owner: '成员 2',
  routes: [
    GoRoute(
      path: '/merchant',
      builder: (context, state) => const MerchantHomePage(),
    ),
    GoRoute(
      path: '/merchant/staff',
      builder: (context, state) => const MerchantStaffPage(),
    ),
    GoRoute(
      path: '/merchant/stats',
      builder: (context, state) => const MerchantStatsPage(),
    ),
  ],
);
