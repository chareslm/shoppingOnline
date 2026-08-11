import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module.dart';
import 'package:shopping_app/features/account/module.dart';
import 'package:shopping_app/features/merchant/module.dart';
import 'package:shopping_app/features/message/module.dart';
import 'package:shopping_app/features/product/module.dart';
import 'package:shopping_app/features/trade/module.dart';

final appModules = <AppModuleContribution>[
  accountModule,
  merchantModule,
  productModule,
  tradeModule,
  messageModule,
];

final appModuleRoutes = <RouteBase>[
  for (final module in appModules) ...module.routes,
];
