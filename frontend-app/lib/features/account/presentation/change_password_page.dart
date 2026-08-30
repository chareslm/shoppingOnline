import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/validation/password_validator.dart';

class ChangePasswordPage extends ConsumerStatefulWidget {
  const ChangePasswordPage({super.key, this.forced = false});

  final bool forced;

  @override
  ConsumerState<ChangePasswordPage> createState() => _ChangePasswordPageState();
}

class _ChangePasswordPageState extends ConsumerState<ChangePasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _currentController = TextEditingController();
  final _newController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _submitting = false;
  bool _obscure = true;

  @override
  void dispose() {
    _currentController.dispose();
    _newController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(authRepositoryProvider)
          .changePassword(
            currentPassword: _currentController.text,
            newPassword: _newController.text,
          );
      if (mounted) context.go('/login?passwordChanged=1');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(widget.forced ? '首次登录修改密码' : '修改密码'),
      automaticallyImplyLeading: !widget.forced,
    ),
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
                  Text(
                    widget.forced
                        ? '新商家账号须先修改临时密码后才能继续使用。修改成功后所有设备会话将失效。'
                        : '修改成功后，所有设备的 Refresh Token 将失效，需要重新登录。',
                  ),
                  const SizedBox(height: 20),
                  TextFormField(
                    controller: _currentController,
                    enabled: !_submitting,
                    obscureText: _obscure,
                    decoration: const InputDecoration(labelText: '当前密码'),
                    validator: (value) =>
                        value == null || value.isEmpty ? '请输入当前密码' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _newController,
                    enabled: !_submitting,
                    obscureText: _obscure,
                    decoration: InputDecoration(
                      labelText: '新密码',
                      helperText: '12–64 位，包含大小写字母、数字和特殊字符',
                      helperMaxLines: 2,
                      suffixIcon: IconButton(
                        onPressed: () => setState(() => _obscure = !_obscure),
                        icon: Icon(
                          _obscure
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                      ),
                    ),
                    validator: validateStrongPassword,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _confirmController,
                    enabled: !_submitting,
                    obscureText: _obscure,
                    decoration: const InputDecoration(labelText: '确认新密码'),
                    validator: (value) =>
                        value != _newController.text ? '两次输入的新密码不一致' : null,
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: _submitting
                        ? const SizedBox.square(
                            dimension: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('确认修改'),
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
