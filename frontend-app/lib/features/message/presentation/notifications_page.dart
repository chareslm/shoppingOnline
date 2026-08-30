import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/message/domain/message_models.dart';

class NotificationsPage extends ConsumerStatefulWidget {
  const NotificationsPage({super.key});

  @override
  ConsumerState<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends ConsumerState<NotificationsPage> {
  List<NotificationItem> _items = const [];
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
      final items = await ref.read(messageApiProvider).listNotifications();
      if (mounted) setState(() => _items = items);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('消息通知'),
      actions: [
        TextButton(
          onPressed: () async {
            try {
              await ref.read(messageApiProvider).markAllNotificationsRead();
              await _load();
            } catch (error) {
              if (context.mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(SnackBar(content: Text(error.toString())));
              }
            }
          },
          child: const Text('全部已读'),
        ),
      ],
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
          : _items.isEmpty
          ? const ListView(children: [Center(child: Text('暂无通知'))])
          : ListView.builder(
              itemCount: _items.length,
              itemBuilder: (context, index) {
                final item = _items[index];
                return ListTile(
                  title: Text(item.title),
                  subtitle: Text(item.content),
                  trailing: item.isRead == 1 ? null : const Icon(Icons.circle, size: 10),
                  onTap: () async {
                    if (item.isRead == 1) return;
                    try {
                      await ref
                          .read(messageApiProvider)
                          .markNotificationRead(item.id);
                      await _load();
                    } catch (error) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(error.toString())),
                        );
                      }
                    }
                  },
                );
              },
            ),
    ),
  );
}
