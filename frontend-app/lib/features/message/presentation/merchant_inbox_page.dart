import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/features/message/domain/message_models.dart';

class MerchantInboxPage extends ConsumerStatefulWidget {
  const MerchantInboxPage({super.key});

  @override
  ConsumerState<MerchantInboxPage> createState() => _MerchantInboxPageState();
}

class _MerchantInboxPageState extends ConsumerState<MerchantInboxPage> {
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
      final roles = ref.read(authSessionProvider).user?.roles ?? const [];
      final api = ref.read(messageApiProvider);
      final sessions = isCustomerServiceOnly(roles)
          ? await api.listCsSessions()
          : await api.listMySessions();
      if (mounted) setState(() => _sessions = sessions);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _assign(ChatSession session) async {
    try {
      await ref.read(messageApiProvider).assignSession(session.sessionId);
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
    appBar: AppBar(title: const Text('用户沟通')),
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
          ? const ListView(children: [Center(child: Text('暂无待处理会话'))])
          : ListView.builder(
              itemCount: _sessions.length,
              itemBuilder: (context, index) {
                final session = _sessions[index];
                return ListTile(
                  title: Text(session.subject ?? '用户会话'),
                  subtitle: Text(session.lastMessage ?? sessionStatusLabels[session.status] ?? ''),
                  trailing: session.csUserId == null
                      ? TextButton(
                          onPressed: () => _assign(session),
                          child: const Text('接入'),
                        )
                      : const Icon(Icons.chevron_right),
                  onTap: () => context.push('/chat/${session.sessionId}'),
                );
              },
            ),
    ),
  );
}
