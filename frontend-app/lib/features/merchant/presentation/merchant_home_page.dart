import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/features/merchant/domain/merchant_models.dart';

class MerchantHomePage extends ConsumerStatefulWidget {
  const MerchantHomePage({super.key});

  @override
  ConsumerState<MerchantHomePage> createState() => _MerchantHomePageState();
}

class _MerchantHomePageState extends ConsumerState<MerchantHomePage> {
  ShopSummary? _shop;
  bool _loggingOut = false;
  bool _switching = false;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_loadShop);
  }

  Future<void> _loadShop() async {
    try {
      final shop = await ref.read(merchantApiProvider).currentShop();
      if (mounted) setState(() => _shop = shop);
    } catch (_) {}
  }

  Future<void> _logout() async {
    setState(() => _loggingOut = true);
    try {
      await ref.read(authRepositoryProvider).logout();
    } finally {
      if (mounted) setState(() => _loggingOut = false);
    }
  }

  Future<void> _switchToUser() async {
    setState(() => _switching = true);
    try {
      await ref.read(authRepositoryProvider).switchPortal(PortalMode.user);
      if (mounted) context.go('/');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _switching = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authSessionProvider);
    final roles = session.user?.roles ?? const [];
    final csOnly = isCustomerServiceOnly(roles);
    final owner = roles.contains('MERCHANT_OWNER');
    final hasUser = roles.contains('USER');
    return Scaffold(
      appBar: AppBar(
        title: Text(csOnly ? '客服工作台' : '商家工作台'),
        actions: [
          IconButton(
            onPressed: _loggingOut ? null : _logout,
            tooltip: '退出登录',
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Card(
            child: ListTile(
              title: Text(_shop?.name ?? session.user?.username ?? '店铺'),
              subtitle: Text(csOnly ? '仅可处理用户沟通' : '本店经营入口'),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.forum_outlined),
                  title: const Text('用户沟通'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/merchant/inbox'),
                ),
                if (!csOnly) ...[
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.inventory_2_outlined),
                    title: const Text('本店商品'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => context.push('/merchant/products'),
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.add_box_outlined),
                    title: const Text('添加商品'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => context.push('/merchant/add-product'),
                  ),
                  if (owner) ...[
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.badge_outlined),
                      title: const Text('客服账号'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/merchant/staff'),
                    ),
                  ],
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.query_stats_outlined),
                    title: const Text('经营统计'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => context.push('/merchant/stats'),
                  ),
                ],
                if (hasUser) ...[
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.swap_horiz_outlined),
                    title: const Text('切换到用户身份'),
                    trailing: _switching
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.chevron_right),
                    onTap: _switching ? null : _switchToUser,
                  ),
                ],
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.logout),
                  title: const Text('退出登录'),
                  onTap: _loggingOut ? null : _logout,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
