import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/auth/session.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key, this.passwordChanged = false});

  final bool passwordChanged;

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _identifierController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _submitting = false;
  bool _obscurePassword = true;
  PortalMode _portalMode = PortalMode.user;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final query = GoRouterState.of(context).uri.queryParameters;
      if (query['portal'] == 'merchant') {
        setState(() => _portalMode = PortalMode.merchant);
      }
      if (widget.passwordChanged) _showError('密码修改成功，请使用新密码重新登录');
    });
  }

  @override
  void dispose() {
    _identifierController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(authRepositoryProvider)
          .login(
            identifier: _identifierController.text.trim(),
            password: _passwordController.text,
            portalMode: _portalMode,
          );
    } catch (error) {
      if (mounted) _showError(error.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(
                    Icons.shopping_bag_outlined,
                    size: 64,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  const SizedBox(height: 20),
                  Text(
                    '登录综合电商平台',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 8),
                  const Text('使用用户名、邮箱或手机号登录，并选择本次进入的身份', textAlign: TextAlign.center),
                  const SizedBox(height: 20),
                  SegmentedButton<PortalMode>(
                    segments: const [
                      ButtonSegment(
                        value: PortalMode.user,
                        label: Text('用户身份'),
                        icon: Icon(Icons.person_outline),
                      ),
                      ButtonSegment(
                        value: PortalMode.merchant,
                        label: Text('商家身份'),
                        icon: Icon(Icons.storefront_outlined),
                      ),
                    ],
                    selected: {_portalMode},
                    onSelectionChanged: _submitting
                        ? null
                        : (value) => setState(() => _portalMode = value.first),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _portalMode == PortalMode.merchant
                        ? '商家主账号管理店铺。客服账号也请选择商家身份，登录后只能进入用户沟通。'
                        : '购物与个人中心。商家与客服请切换到商家身份。',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 20),
                  TextFormField(
                    controller: _identifierController,
                    enabled: !_submitting,
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(
                      labelText: '账号',
                      prefixIcon: Icon(Icons.person_outline),
                    ),
                    validator: (value) =>
                        value == null || value.trim().isEmpty ? '请输入账号' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _passwordController,
                    enabled: !_submitting,
                    obscureText: _obscurePassword,
                    onFieldSubmitted: (_) => _submit(),
                    decoration: InputDecoration(
                      labelText: '密码',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        onPressed: () => setState(
                          () => _obscurePassword = !_obscurePassword,
                        ),
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                      ),
                    ),
                    validator: (value) =>
                        value == null || value.isEmpty ? '请输入密码' : null,
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: _submitting
                          ? const SizedBox.square(
                              dimension: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('登录'),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: _submitting
                        ? null
                        : () => context.go(
                            _portalMode == PortalMode.merchant
                                ? '/register?account=merchant'
                                : '/register',
                          ),
                    child: const Text('没有账号？立即注册'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    ),
  );
}
