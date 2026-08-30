import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/auth/session.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  bool _loggingOut = false;
  bool _switching = false;

  Future<void> _logout() async {
    setState(() => _loggingOut = true);
    try {
      await ref.read(authRepositoryProvider).logout();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('服务端退出失败，本地会话已清除：$error')));
      }
    } finally {
      if (mounted) setState(() => _loggingOut = false);
    }
  }

  Future<void> _enterMerchant() async {
    setState(() => _switching = true);
    try {
      await ref.read(authRepositoryProvider).switchPortal(PortalMode.merchant);
      if (mounted) {
        final roles = ref.read(authSessionProvider).user?.roles ?? const [];
        context.go(portalHomePath(PortalMode.merchant, roles));
      }
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
    final user = ref.watch(authSessionProvider).user;
    final canEnterMerchant = hasMerchantPortalRole(user?.roles ?? const []);
    return Scaffold(
      appBar: AppBar(
        title: const Text('综合电商平台'),
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
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '你好，${user?.username ?? ''}',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 12),
                  Text('用户 ID：${user?.userId ?? '-'}'),
                  const SizedBox(height: 6),
                  Text(
                    '角色：${user?.roles.isEmpty ?? true ? '暂无' : user!.roles.join('、')}',
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.storefront_outlined),
                  title: const Text('商品'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/products'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.shopping_cart_outlined),
                  title: const Text('购物车'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/cart'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.receipt_long_outlined),
                  title: const Text('订单'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/orders'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.support_agent_outlined),
                  title: const Text('客服沟通'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/chat'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.notifications_outlined),
                  title: const Text('消息通知'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/notifications'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.badge_outlined),
                  title: const Text('个人资料'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/profile'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.location_on_outlined),
                  title: const Text('收货地址'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/addresses'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.tune_outlined),
                  title: const Text('偏好设置'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/preferences'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.password_outlined),
                  title: const Text('修改密码'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/change-password'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.devices_outlined),
                  title: const Text('登录设备'),
                  subtitle: const Text('查看并退出其他设备'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/devices'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.query_stats_outlined),
                  title: const Text('消费统计'),
                  subtitle: const Text('查看本人支付、退款与评价概览'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/statistics'),
                ),
                if (canEnterMerchant) ...[
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.swap_horiz_outlined),
                    title: const Text('进入商家工作台'),
                    subtitle: const Text('切换为商家或客服身份'),
                    trailing: _switching
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.chevron_right),
                    onTap: _switching ? null : _enterMerchant,
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
