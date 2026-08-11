import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';

class PreferencePage extends ConsumerStatefulWidget {
  const PreferencePage({super.key});

  @override
  ConsumerState<PreferencePage> createState() => _PreferencePageState();
}

class _PreferencePageState extends ConsumerState<PreferencePage> {
  UserPreference? _preference;
  String? _error;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final preference = await ref.read(userRepositoryProvider).preference();
      if (mounted) setState(() => _preference = preference);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    }
  }

  Future<void> _save() async {
    final current = _preference;
    if (current == null) return;
    setState(() => _saving = true);
    try {
      final updated = await ref
          .read(userRepositoryProvider)
          .updatePreference(
            marketingEnabled: current.marketingEnabled,
            orderNotificationEnabled: current.orderNotificationEnabled,
            systemNotificationEnabled: current.systemNotificationEnabled,
            extraPreferences: current.extraPreferences,
          );
      if (!mounted) return;
      setState(() => _preference = updated);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('偏好设置已保存')));
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _replace({bool? marketing, bool? order, bool? system}) {
    final current = _preference!;
    setState(() {
      _preference = UserPreference(
        userId: current.userId,
        marketingEnabled: marketing ?? current.marketingEnabled,
        orderNotificationEnabled: order ?? current.orderNotificationEnabled,
        systemNotificationEnabled: system ?? current.systemNotificationEnabled,
        extraPreferences: current.extraPreferences,
      );
    });
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('偏好设置')),
    body: _preference == null
        ? Center(
            child: _error == null
                ? const CircularProgressIndicator()
                : Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(_error!),
                      const SizedBox(height: 12),
                      FilledButton(onPressed: _load, child: const Text('重试')),
                    ],
                  ),
          )
        : ListView(
            padding: const EdgeInsets.all(20),
            children: [
              SwitchListTile(
                title: const Text('营销消息'),
                subtitle: const Text('接收活动和优惠信息'),
                value: _preference!.marketingEnabled,
                onChanged: _saving
                    ? null
                    : (value) => _replace(marketing: value),
              ),
              SwitchListTile(
                title: const Text('订单通知'),
                subtitle: const Text('接收订单状态变化通知'),
                value: _preference!.orderNotificationEnabled,
                onChanged: _saving ? null : (value) => _replace(order: value),
              ),
              SwitchListTile(
                title: const Text('系统通知'),
                subtitle: const Text('接收平台重要通知'),
                value: _preference!.systemNotificationEnabled,
                onChanged: _saving ? null : (value) => _replace(system: value),
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: _saving ? null : _save,
                icon: _saving
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.save_outlined),
                label: const Text('保存偏好'),
              ),
            ],
          ),
  );
}
