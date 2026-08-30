import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/auth/session.dart';

class ForbiddenPage extends ConsumerWidget {
  const ForbiddenPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authSessionProvider);
    final user = session.user;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.block_outlined,
                    size: 64,
                    color: Theme.of(context).colorScheme.error,
                  ),
                  const SizedBox(height: 20),
                  Text(
                    session.portalMode == PortalMode.merchant
                        ? '当前账号不能访问商家工作台'
                        : '当前账号不能访问用户端',
                    style: Theme.of(context).textTheme.headlineSmall,
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    session.portalMode == PortalMode.merchant
                        ? '商家身份要求 MERCHANT_OWNER、MERCHANT_STAFF 或 CUSTOMER_SERVICE。当前角色：${user?.roles.join('、') ?? '无'}'
                        : 'Android 用户端要求账号具有 USER 角色。当前角色：${user?.roles.join('、') ?? '无'}',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 24),
                  FilledButton.icon(
                    onPressed: () => ref.read(authRepositoryProvider).logout(),
                    icon: const Icon(Icons.logout),
                    label: const Text('退出并更换账号'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
