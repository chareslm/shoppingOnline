import 'package:go_router/go_router.dart';

class AppModuleContribution {
  const AppModuleContribution({
    required this.key,
    required this.owner,
    this.routes = const [],
  });

  final String key;
  final String owner;
  final List<RouteBase> routes;
}
