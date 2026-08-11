import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module.dart';
import 'package:shopping_app/features/account/presentation/address_page.dart';
import 'package:shopping_app/features/account/presentation/change_password_page.dart';
import 'package:shopping_app/features/account/presentation/home_page.dart';
import 'package:shopping_app/features/account/presentation/preference_page.dart';
import 'package:shopping_app/features/account/presentation/profile_page.dart';

final accountModule = AppModuleContribution(
  key: 'account',
  owner: '项目管理员',
  routes: [
    GoRoute(path: '/', builder: (context, state) => const HomePage()),
    GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
    GoRoute(
      path: '/addresses',
      builder: (context, state) => const AddressPage(),
    ),
    GoRoute(
      path: '/preferences',
      builder: (context, state) => const PreferencePage(),
    ),
    GoRoute(
      path: '/change-password',
      builder: (context, state) => const ChangePasswordPage(),
    ),
  ],
);
