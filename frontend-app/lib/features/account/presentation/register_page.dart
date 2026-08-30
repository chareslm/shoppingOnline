import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/core/auth/session.dart';
import 'package:shopping_app/core/validation/password_validator.dart';
import 'package:shopping_app/features/merchant/domain/merchant_models.dart';

class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  final _shopNameController = TextEditingController();
  final _subjectNameController = TextEditingController();
  final _creditCodeController = TextEditingController();
  final _principalController = TextEditingController();
  final _idNumberController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  bool _submitting = false;
  bool _obscurePassword = true;
  PortalMode _accountType = PortalMode.user;
  String _merchantType = 'ENTERPRISE';
  String _idType = 'MAINLAND_ID_CARD';
  List<PlatformFile> _files = const [];
  String? _successMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final account = GoRouterState.of(context).uri.queryParameters['account'];
      if (account == 'merchant' && mounted) {
        setState(() => _accountType = PortalMode.merchant);
      }
    });
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    _shopNameController.dispose();
    _subjectNameController.dispose();
    _creditCodeController.dispose();
    _principalController.dispose();
    _idNumberController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _pickFiles() async {
    final result = await FilePicker.platform.pickFiles(
      allowMultiple: true,
      type: FileType.custom,
      allowedExtensions: const ['pdf', 'jpg', 'jpeg', 'png'],
      withData: false,
    );
    if (result == null) return;
    setState(() => _files = result.files.take(5).toList(growable: false));
  }

  Future<void> _submit() async {
    if (_accountType == PortalMode.user) {
      if (!_formKey.currentState!.validate()) return;
    } else {
      final error = _validateMerchant();
      if (error != null) {
        _showMessage(error);
        return;
      }
    }
    setState(() => _submitting = true);
    try {
      if (_accountType == PortalMode.merchant) {
        final receipt = await ref
            .read(merchantApiProvider)
            .submitApplication(
              MerchantApplicationRequest(
                merchantType: _merchantType,
                shopName: _shopNameController.text.trim(),
                subjectName: _subjectNameController.text.trim().isEmpty
                    ? null
                    : _subjectNameController.text.trim(),
                unifiedSocialCreditCode: _creditCodeController.text
                    .trim()
                    .toUpperCase()
                    .isEmpty
                    ? null
                    : _creditCodeController.text.trim().toUpperCase(),
                responsiblePersonName: _principalController.text.trim(),
                identityDocumentType: _idType,
                identityDocumentNumber: _idNumberController.text.trim(),
                contactPhone: _phoneController.text.trim(),
                contactEmail: _emailController.text.trim().toLowerCase(),
              ),
              _files,
            );
        if (!mounted) return;
        setState(
          () => _successMessage =
              '申请 ${receipt.id} 已提交。资质审核通过后，系统会向申请邮箱发送开通通知。',
        );
        return;
      }
      final user = await ref
          .read(authRepositoryProvider)
          .register(
            username: _usernameController.text.trim(),
            password: _passwordController.text,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('${user.username} 注册成功，请登录')));
      context.go('/login');
    } catch (error) {
      if (mounted) _showMessage(error.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String? _validateMerchant() {
    if (_shopNameController.text.trim().isEmpty) return '请输入店铺名称';
    if (_merchantType != 'INDIVIDUAL' &&
        _subjectNameController.text.trim().isEmpty) {
      return '请输入经营主体名称';
    }
    if (_merchantType != 'INDIVIDUAL' &&
        !RegExp(
          r'^[0-9A-Z]{18}$',
        ).hasMatch(_creditCodeController.text.trim().toUpperCase())) {
      return '请输入正确的 18 位统一社会信用代码';
    }
    if (_principalController.text.trim().isEmpty) return '请输入负责人或经营者姓名';
    if (_idNumberController.text.trim().isEmpty) return '请输入身份凭证号码';
    if (!RegExp(r'^1\d{10}$').hasMatch(_phoneController.text.trim())) {
      return '请输入正确的 11 位中国大陆手机号';
    }
    if (!RegExp(
      r'^[^\s@]+@[^\s@]+\.[^\s@]+$',
    ).hasMatch(_emailController.text.trim())) {
      return '请输入正确的邮箱地址';
    }
    if (_files.isEmpty) return '请上传至少一份资质许可文件';
    if (_files.length > 5) return '资质文件最多上传 5 份';
    const maxBytes = 10 * 1024 * 1024;
    for (final file in _files) {
      final size = file.size;
      if (size > maxBytes) return '文件 ${file.name} 超过 10 MB';
    }
    return null;
  }

  String? _validateUsername(String? value) {
    final username = value?.trim() ?? '';
    if (username.isEmpty) return '请输入用户名';
    if (!RegExp(r'^[A-Za-z][A-Za-z0-9_]{2,63}$').hasMatch(username)) {
      return '用户名须以字母开头，长度 3–64 位';
    }
    return null;
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(_accountType == PortalMode.merchant ? '申请商家入驻' : '注册账号'),
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
                  SegmentedButton<PortalMode>(
                    segments: const [
                      ButtonSegment(
                        value: PortalMode.user,
                        label: Text('个人账号'),
                      ),
                      ButtonSegment(
                        value: PortalMode.merchant,
                        label: Text('商家入驻'),
                      ),
                    ],
                    selected: {_accountType},
                    onSelectionChanged: _submitting
                        ? null
                        : (value) => setState(() {
                            _accountType = value.first;
                            _successMessage = null;
                          }),
                  ),
                  const SizedBox(height: 20),
                  if (_accountType == PortalMode.user) ...[
                    TextFormField(
                      controller: _usernameController,
                      enabled: !_submitting,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                        labelText: '用户名',
                        prefixIcon: Icon(Icons.person_add_alt_1_outlined),
                      ),
                      validator: _accountType == PortalMode.user
                          ? _validateUsername
                          : null,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _passwordController,
                      enabled: !_submitting,
                      obscureText: _obscurePassword,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(
                        labelText: '密码',
                        helperText: '12–64 位，包含大小写字母、数字和特殊字符',
                        helperMaxLines: 2,
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
                      validator: _accountType == PortalMode.user
                          ? validateStrongPassword
                          : null,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _confirmController,
                      enabled: !_submitting,
                      obscureText: _obscurePassword,
                      onFieldSubmitted: (_) => _submit(),
                      decoration: const InputDecoration(
                        labelText: '确认密码',
                        prefixIcon: Icon(Icons.verified_user_outlined),
                      ),
                      validator: _accountType == PortalMode.user
                          ? (value) => value != _passwordController.text
                                ? '两次输入的密码不一致'
                                : null
                          : null,
                    ),
                  ] else ...[
                    DropdownButtonFormField<String>(
                      value: _merchantType,
                      decoration: const InputDecoration(labelText: '商家类型'),
                      items: const [
                        DropdownMenuItem(value: 'ENTERPRISE', child: Text('企业')),
                        DropdownMenuItem(
                          value: 'SOLE_PROPRIETOR',
                          child: Text('个体工商户'),
                        ),
                        DropdownMenuItem(
                          value: 'INDIVIDUAL',
                          child: Text('个人商家'),
                        ),
                      ],
                      onChanged: _submitting
                          ? null
                          : (value) =>
                                setState(() => _merchantType = value ?? 'ENTERPRISE'),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _shopNameController,
                      enabled: !_submitting,
                      decoration: const InputDecoration(labelText: '店铺名称'),
                    ),
                    if (_merchantType != 'INDIVIDUAL') ...[
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _subjectNameController,
                        enabled: !_submitting,
                        decoration: const InputDecoration(labelText: '经营主体名称'),
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _creditCodeController,
                        enabled: !_submitting,
                        decoration: const InputDecoration(
                          labelText: '统一社会信用代码',
                        ),
                      ),
                    ],
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _principalController,
                      enabled: !_submitting,
                      decoration: const InputDecoration(
                        labelText: '负责人 / 经营者姓名',
                      ),
                    ),
                    const SizedBox(height: 16),
                    DropdownButtonFormField<String>(
                      value: _idType,
                      decoration: const InputDecoration(labelText: '证件类型'),
                      items: const [
                        DropdownMenuItem(
                          value: 'MAINLAND_ID_CARD',
                          child: Text('中国大陆居民身份证'),
                        ),
                        DropdownMenuItem(value: 'PASSPORT', child: Text('护照')),
                        DropdownMenuItem(value: 'OTHER', child: Text('其他有效证件')),
                      ],
                      onChanged: _submitting
                          ? null
                          : (value) =>
                                setState(() => _idType = value ?? 'MAINLAND_ID_CARD'),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _idNumberController,
                      enabled: !_submitting,
                      decoration: const InputDecoration(labelText: '证件号码'),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _phoneController,
                      enabled: !_submitting,
                      keyboardType: TextInputType.phone,
                      decoration: const InputDecoration(labelText: '手机号'),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _emailController,
                      enabled: !_submitting,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(labelText: '邮箱'),
                    ),
                    const SizedBox(height: 16),
                    OutlinedButton.icon(
                      onPressed: _submitting ? null : _pickFiles,
                      icon: const Icon(Icons.attach_file),
                      label: Text(
                        _files.isEmpty
                            ? '选择资质文件（PDF/JPG/PNG，最多 5 份）'
                            : '已选择 ${_files.length} 份：${_files.map((file) => file.name).join('、')}',
                      ),
                    ),
                    if (_successMessage != null) ...[
                      const SizedBox(height: 16),
                      Text(
                        _successMessage!,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.primary,
                        ),
                      ),
                    ],
                  ],
                  const SizedBox(height: 24),
                  if (_successMessage == null)
                    FilledButton(
                      onPressed: _submitting ? null : _submit,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        child: _submitting
                            ? const SizedBox.square(
                                dimension: 20,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : Text(
                                _accountType == PortalMode.merchant
                                    ? '提交入驻申请'
                                    : '注册',
                              ),
                      ),
                    ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: _submitting
                        ? null
                        : () => context.go(
                            _accountType == PortalMode.merchant
                                ? '/login?portal=merchant'
                                : '/login',
                          ),
                    child: const Text('已有账号？返回登录'),
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
