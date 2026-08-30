import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/module.dart';
import 'package:shopping_app/features/message/presentation/chat_list_page.dart';
import 'package:shopping_app/features/message/presentation/chat_thread_page.dart';
import 'package:shopping_app/features/message/presentation/merchant_inbox_page.dart';
import 'package:shopping_app/features/message/presentation/notifications_page.dart';

final messageModule = AppModuleContribution(
  key: 'message',
  owner: '成员 5',
  routes: [
    GoRoute(path: '/chat', builder: (context, state) => const ChatListPage()),
    GoRoute(
      path: '/chat/:sessionId',
      builder: (context, state) =>
          ChatThreadPage(sessionId: state.pathParameters['sessionId']!),
    ),
    GoRoute(
      path: '/notifications',
      builder: (context, state) => const NotificationsPage(),
    ),
    GoRoute(
      path: '/merchant/inbox',
      builder: (context, state) => const MerchantInboxPage(),
    ),
  ],
);
