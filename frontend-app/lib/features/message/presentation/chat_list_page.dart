import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/message/domain/message_models.dart';

class ChatListPage extends ConsumerStatefulWidget {
  const ChatListPage({super.key});

  @override
  ConsumerState<ChatListPage> createState() => _ChatListPageState();
}

class _ChatListPageState extends ConsumerState<ChatListPage> {
  List<ChatSession> _sessions = const [];
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
      final sessions = await ref.read(messageApiProvider).listMySessions();
      if (mounted) setState(() => _sessions = sessions);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _create() async {
    final subject = TextEditingController();
    final message = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('发起客服沟通'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: subject,
              decoration: const InputDecoration(labelText: '主题（可选）'),
            ),
            TextField(
              controller: message,
              decoration: const InputDecoration(labelText: '首条消息'),
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
      final session = await ref
          .read(messageApiProvider)
          .createSession(
            subject: subject.text.trim(),
            firstMessage: message.text.trim(),
          );
      if (mounted) context.push('/chat/${session.sessionId}');
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
    appBar: AppBar(title: const Text('客服沟通')),
    floatingActionButton: FloatingActionButton(
      onPressed: _create,
      tooltip: '发起会话',
      child: const Icon(Icons.add),
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
          : _sessions.isEmpty
          ? const ListView(children: [Center(child: Text('暂无会话'))])
          : ListView.builder(
              itemCount: _sessions.length,
              itemBuilder: (context, index) {
                final session = _sessions[index];
                return ListTile(
                  title: Text(session.subject ?? '客服会话'),
                  subtitle: Text(
                    session.lastMessage ?? sessionStatusLabels[session.status] ?? '',
                  ),
                  trailing: session.unreadCount > 0
                      ? Badge(label: Text('${session.unreadCount}'))
                      : null,
                  onTap: () => context.push('/chat/${session.sessionId}'),
                );
              },
            ),
    ),
  );
}
