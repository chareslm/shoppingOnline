import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({super.key});

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  final _formKey = GlobalKey<FormState>();
  final _nicknameController = TextEditingController();
  final _avatarController = TextEditingController();
  final _realNameController = TextEditingController();
  final _bioController = TextEditingController();
  UserProfile? _profile;
  DateTime? _birthday;
  String _gender = 'UNKNOWN';
  String? _error;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _avatarController.dispose();
    _realNameController.dispose();
    _bioController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final profile = await ref.read(userRepositoryProvider).profile();
      if (!mounted) return;
      _nicknameController.text = profile.nickname ?? '';
      _avatarController.text = profile.avatarUrl ?? '';
      _realNameController.text = profile.realName ?? '';
      _bioController.text = profile.bio ?? '';
      setState(() {
        _profile = profile;
        _birthday = profile.birthday;
        _gender = profile.gender;
      });
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      final profile = await ref
          .read(userRepositoryProvider)
          .updateProfile(
            nickname: _nicknameController.text.trim(),
            avatarUrl: _avatarController.text.trim(),
            realName: _realNameController.text.trim(),
            gender: _gender,
            birthday: _birthday,
            bio: _bioController.text.trim(),
          );
      if (!mounted) return;
      setState(() => _profile = profile);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('个人资料已保存')));
    } catch (error) {
      if (mounted) _showError(error.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _pickBirthday() async {
    final now = DateTime.now();
    final selected = await showDatePicker(
      context: context,
      initialDate: _birthday ?? DateTime(now.year - 20),
      firstDate: DateTime(1900),
      lastDate: now.subtract(const Duration(days: 1)),
    );
    if (selected != null && mounted) setState(() => _birthday = selected);
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('个人资料')),
      body: _profile == null
          ? Center(
              child: _error == null
                  ? const CircularProgressIndicator()
                  : Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(_error!, textAlign: TextAlign.center),
                          const SizedBox(height: 16),
                          FilledButton(
                            onPressed: _load,
                            child: const Text('重试'),
                          ),
                        ],
                      ),
                    ),
            )
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  TextFormField(
                    controller: _nicknameController,
                    enabled: !_saving,
                    maxLength: 64,
                    decoration: const InputDecoration(labelText: '昵称'),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _realNameController,
                    enabled: !_saving,
                    maxLength: 64,
                    decoration: const InputDecoration(labelText: '真实姓名'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: _gender,
                    decoration: const InputDecoration(labelText: '性别'),
                    items: const [
                      DropdownMenuItem(value: 'UNKNOWN', child: Text('未设置')),
                      DropdownMenuItem(value: 'MALE', child: Text('男')),
                      DropdownMenuItem(value: 'FEMALE', child: Text('女')),
                    ],
                    onChanged: _saving
                        ? null
                        : (value) => setState(() => _gender = value!),
                  ),
                  const SizedBox(height: 12),
                  InkWell(
                    onTap: _saving ? null : _pickBirthday,
                    child: InputDecorator(
                      decoration: const InputDecoration(labelText: '生日'),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              _birthday == null
                                  ? '未设置'
                                  : _formatDate(_birthday!),
                            ),
                          ),
                          if (_birthday != null)
                            IconButton(
                              onPressed: _saving
                                  ? null
                                  : () => setState(() => _birthday = null),
                              icon: const Icon(Icons.clear),
                              tooltip: '清除生日',
                            ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _avatarController,
                    enabled: !_saving,
                    maxLength: 512,
                    keyboardType: TextInputType.url,
                    decoration: const InputDecoration(labelText: '头像地址'),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _bioController,
                    enabled: !_saving,
                    maxLength: 500,
                    maxLines: 4,
                    decoration: const InputDecoration(labelText: '个人简介'),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: _saving ? null : _save,
                    icon: _saving
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.save_outlined),
                    label: const Text('保存资料'),
                  ),
                ],
              ),
            ),
    );
  }
}

String _formatDate(DateTime value) =>
    '${value.year.toString().padLeft(4, '0')}-'
    '${value.month.toString().padLeft(2, '0')}-'
    '${value.day.toString().padLeft(2, '0')}';
