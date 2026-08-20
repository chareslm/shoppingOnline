import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/account/domain/auth_models.dart';

class DeviceSessionsPage extends ConsumerStatefulWidget {
  const DeviceSessionsPage({super.key});

  @override
  ConsumerState<DeviceSessionsPage> createState() => _DeviceSessionsPageState();
}

class _DeviceSessionsPageState extends ConsumerState<DeviceSessionsPage> {
  List<DeviceSession>? _devices;
  String? _error;
  String? _actingId;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final devices = await ref.read(authRepositoryProvider).devices();
      if (mounted) setState(() => _devices = devices);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    }
  }

  Future<void> _revoke(DeviceSession device) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(device.current ? '退出当前设备？' : '退出这台设备？'),
        content: const Text('该设备的 Refresh Token 将立即失效，无法继续刷新登录状态。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确认退出'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _actingId = device.id);
    try {
      await ref
          .read(authRepositoryProvider)
          .revokeDevice(device.id, current: device.current);
      if (!mounted || device.current) return;
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('指定设备已退出')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _actingId = null);
    }
  }

  Future<void> _revokeOthers() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('退出其他设备？'),
        content: const Text('除当前设备外，其他设备的登录会话都会失效。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('全部退出'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _actingId = 'others');
    try {
      await ref.read(authRepositoryProvider).revokeOtherDevices();
      if (!mounted) return;
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('其他设备已全部退出')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _actingId = null);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('登录设备'),
      actions: [
        TextButton(
          onPressed: _actingId == null ? _revokeOthers : null,
          child: const Text('退出其他设备'),
        ),
      ],
    ),
    body: _devices == null
        ? Center(
            child: _error == null
                ? const CircularProgressIndicator()
                : Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(24),
                        child: Text(_error!, textAlign: TextAlign.center),
                      ),
                      FilledButton(onPressed: _load, child: const Text('重试')),
                    ],
                  ),
          )
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                for (final device in _devices!)
                  _DeviceCard(
                    device: device,
                    busy: _actingId != null,
                    onRevoke: () => _revoke(device),
                  ),
                const Padding(
                  padding: EdgeInsets.all(12),
                  child: Text(
                    '退出后 Refresh Token 会立即失效；已签发的 Access Token 最长仍可能使用 30 分钟。',
                    style: TextStyle(color: Colors.black54),
                  ),
                ),
              ],
            ),
          ),
  );
}

class _DeviceCard extends StatelessWidget {
  const _DeviceCard({
    required this.device,
    required this.busy,
    required this.onRevoke,
  });

  final DeviceSession device;
  final bool busy;
  final VoidCallback onRevoke;

  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(bottom: 12),
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(_icon(device.deviceType)),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  device.deviceName?.isNotEmpty == true
                      ? device.deviceName!
                      : _label(device.deviceType),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              if (device.current) const Chip(label: Text('当前设备')),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            '${_label(device.deviceType)}${device.appVersion == null ? '' : ' · ${device.appVersion}'}',
          ),
          const SizedBox(height: 6),
          Text(
            '最近活跃：${_format(device.lastActiveAt)} · IP：${device.maskedIp ?? '未知'}',
          ),
          const SizedBox(height: 4),
          Text(
            device.sessionExpiresAt == null
                ? '会话状态：已退出'
                : '会话有效期：${_format(device.sessionExpiresAt!)}',
          ),
          const SizedBox(height: 10),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton.icon(
              onPressed: busy ? null : onRevoke,
              icon: const Icon(Icons.logout),
              label: Text(device.current ? '退出当前设备' : '退出设备'),
            ),
          ),
        ],
      ),
    ),
  );

  static String _label(String type) => switch (type) {
    'ANDROID' => 'Android App',
    'MINIAPP' => '微信小程序',
    'ADMIN_WEB' => '管理端',
    _ => '网页浏览器',
  };

  static IconData _icon(String type) => switch (type) {
    'ANDROID' => Icons.phone_android,
    'MINIAPP' => Icons.chat_bubble_outline,
    _ => Icons.computer,
  };

  static String _format(DateTime value) {
    final local = value.toLocal();
    String two(int number) => number.toString().padLeft(2, '0');
    return '${local.year}-${two(local.month)}-${two(local.day)} '
        '${two(local.hour)}:${two(local.minute)}';
  }
}
