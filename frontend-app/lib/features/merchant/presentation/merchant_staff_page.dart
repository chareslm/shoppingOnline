import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/merchant/domain/merchant_models.dart';

class MerchantStaffPage extends ConsumerStatefulWidget {
  const MerchantStaffPage({super.key});

  @override
  ConsumerState<MerchantStaffPage> createState() => _MerchantStaffPageState();
}

class _MerchantStaffPageState extends ConsumerState<MerchantStaffPage> {
  List<ShopStaffAccount> _staff = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final staff = await ref.read(merchantApiProvider).listStaff();
      if (mounted) setState(() => _staff = staff);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _create() async {
    final email = TextEditingController();
    final name = TextEditingController();
    final username = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('创建客服账号'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: email,
              decoration: const InputDecoration(labelText: '邮箱'),
            ),
            TextField(
              controller: name,
              decoration: const InputDecoration(labelText: '显示名称'),
            ),
            TextField(
              controller: username,
              decoration: const InputDecoration(labelText: '用户名（可选）'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('创建'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await ref
          .read(merchantApiProvider)
          .createStaff(
            email: email.text.trim(),
            displayName: name.text.trim(),
            username: username.text.trim(),
          );
      await _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('客服账号'),
      actions: [IconButton(onPressed: _create, icon: const Icon(Icons.add))],
    ),
    body: RefreshIndicator(
      onRefresh: _load,
      child: _loading
          ? const ListView(
              children: [
                SizedBox(height: 80),
                Center(child: CircularProgressIndicator()),
              ],
            )
          : _error != null
          ? ListView(children: [ListTile(title: Text(_error!))])
          : ListView.builder(
              itemCount: _staff.length,
              itemBuilder: (context, index) {
                final item = _staff[index];
                return ListTile(
                  title: Text(item.displayName),
                  subtitle: Text(
                    '${item.maskedEmail ?? ''}  ${staffStatusLabels[item.status] ?? item.status}',
                  ),
                  trailing: TextButton(
                    onPressed: () async {
                      try {
                        await ref
                            .read(merchantApiProvider)
                            .retryStaffEmail(item.id);
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('已重试发送开通邮件')),
                          );
                        }
                      } catch (error) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text(error.toString())),
                          );
                        }
                      }
                    },
                    child: const Text('重试邮件'),
                  ),
                );
              },
            ),
    ),
  );
}
