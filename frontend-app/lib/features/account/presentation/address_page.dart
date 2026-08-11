import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shopping_app/app/providers.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';

class AddressPage extends ConsumerStatefulWidget {
  const AddressPage({super.key});

  @override
  ConsumerState<AddressPage> createState() => _AddressPageState();
}

class _AddressPageState extends ConsumerState<AddressPage> {
  List<UserAddress>? _addresses;
  String? _error;
  bool _working = false;

  @override
  void initState() {
    super.initState();
    Future<void>.microtask(_load);
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final addresses = await ref.read(userRepositoryProvider).addresses();
      if (mounted) setState(() => _addresses = addresses);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    }
  }

  Future<void> _edit([UserAddress? address]) async {
    final changed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (context) => AddressFormSheet(address: address),
    );
    if (changed == true) await _load();
  }

  Future<void> _setDefault(UserAddress address) async {
    await _run(
      () => ref.read(userRepositoryProvider).setDefaultAddress(address.id),
      '默认地址已更新',
    );
  }

  Future<void> _delete(UserAddress address) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除收货地址'),
        content: Text('确定删除 ${address.recipientName} 的这条地址吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    await _run(
      () => ref.read(userRepositoryProvider).deleteAddress(address.id),
      '地址已删除',
    );
  }

  Future<void> _run(Future<void> Function() action, String success) async {
    setState(() => _working = true);
    try {
      await action();
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(success)));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _working = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('收货地址')),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: _working ? null : () => _edit(),
      icon: const Icon(Icons.add_location_alt_outlined),
      label: const Text('新增地址'),
    ),
    body: _addresses == null
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
        : _addresses!.isEmpty
        ? const Center(child: Text('暂无收货地址，请点击下方按钮添加'))
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: _addresses!.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final address = _addresses![index];
                return Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                '${address.recipientName}  ${address.recipientPhone}',
                                style: Theme.of(context).textTheme.titleMedium,
                              ),
                            ),
                            if (address.isDefault)
                              const Chip(label: Text('默认')),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          '${address.provinceName} ${address.cityName} '
                          '${address.districtName} ${address.detailAddress}',
                        ),
                        if (address.postalCode?.isNotEmpty == true) ...[
                          const SizedBox(height: 4),
                          Text('邮编：${address.postalCode}'),
                        ],
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          children: [
                            TextButton.icon(
                              onPressed: _working ? null : () => _edit(address),
                              icon: const Icon(Icons.edit_outlined),
                              label: const Text('编辑'),
                            ),
                            if (!address.isDefault)
                              TextButton.icon(
                                onPressed: _working
                                    ? null
                                    : () => _setDefault(address),
                                icon: const Icon(Icons.check_circle_outline),
                                label: const Text('设为默认'),
                              ),
                            TextButton.icon(
                              onPressed: _working
                                  ? null
                                  : () => _delete(address),
                              icon: const Icon(Icons.delete_outline),
                              label: const Text('删除'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
  );
}

class AddressFormSheet extends ConsumerStatefulWidget {
  const AddressFormSheet({super.key, this.address});

  final UserAddress? address;

  @override
  ConsumerState<AddressFormSheet> createState() => _AddressFormSheetState();
}

class _AddressFormSheetState extends ConsumerState<AddressFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _phoneController;
  late final TextEditingController _provinceController;
  late final TextEditingController _cityController;
  late final TextEditingController _districtController;
  late final TextEditingController _detailController;
  late final TextEditingController _postalController;
  bool _isDefault = false;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    final address = widget.address;
    _nameController = TextEditingController(text: address?.recipientName ?? '');
    _phoneController = TextEditingController(
      text: address?.recipientPhone ?? '',
    );
    _provinceController = TextEditingController(
      text: address?.provinceName ?? '',
    );
    _cityController = TextEditingController(text: address?.cityName ?? '');
    _districtController = TextEditingController(
      text: address?.districtName ?? '',
    );
    _detailController = TextEditingController(
      text: address?.detailAddress ?? '',
    );
    _postalController = TextEditingController(text: address?.postalCode ?? '');
    _isDefault = address?.isDefault ?? false;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _provinceController.dispose();
    _cityController.dispose();
    _districtController.dispose();
    _detailController.dispose();
    _postalController.dispose();
    super.dispose();
  }

  String? _required(String? value) =>
      value == null || value.trim().isEmpty ? '此项不能为空' : null;

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    final current = widget.address;
    final payload = SaveUserAddress(
      recipientName: _nameController.text.trim(),
      recipientPhone: _phoneController.text.trim(),
      provinceCode: current?.provinceCode ?? '',
      provinceName: _provinceController.text.trim(),
      cityCode: current?.cityCode ?? '',
      cityName: _cityController.text.trim(),
      districtCode: current?.districtCode ?? '',
      districtName: _districtController.text.trim(),
      detailAddress: _detailController.text.trim(),
      postalCode: _postalController.text.trim(),
      isDefault: _isDefault,
    );
    try {
      final repository = ref.read(userRepositoryProvider);
      if (current == null) {
        await repository.createAddress(payload);
      } else {
        await repository.updateAddress(current.id, payload);
      }
      if (mounted) Navigator.pop(context, true);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
    child: SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              widget.address == null ? '新增收货地址' : '编辑收货地址',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 20),
            TextFormField(
              controller: _nameController,
              enabled: !_submitting,
              maxLength: 64,
              decoration: const InputDecoration(labelText: '收货人'),
              validator: _required,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _phoneController,
              enabled: !_submitting,
              maxLength: 32,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: '联系电话'),
              validator: (value) {
                final requiredError = _required(value);
                if (requiredError != null) return requiredError;
                return RegExp(r'^[0-9+() -]{6,32}$').hasMatch(value!.trim())
                    ? null
                    : '联系电话格式不正确';
              },
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: _locationField(_provinceController, '省份')),
                const SizedBox(width: 8),
                Expanded(child: _locationField(_cityController, '城市')),
                const SizedBox(width: 8),
                Expanded(child: _locationField(_districtController, '区县')),
              ],
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _detailController,
              enabled: !_submitting,
              maxLength: 255,
              maxLines: 3,
              decoration: const InputDecoration(labelText: '详细地址'),
              validator: _required,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _postalController,
              enabled: !_submitting,
              maxLength: 16,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: '邮政编码（选填）'),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('设为默认收货地址'),
              value: _isDefault,
              onChanged: _submitting
                  ? null
                  : (value) => setState(() => _isDefault = value),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _submitting ? null : _submit,
              child: _submitting
                  ? const SizedBox.square(
                      dimension: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('保存地址'),
            ),
          ],
        ),
      ),
    ),
  );

  Widget _locationField(TextEditingController controller, String label) =>
      TextFormField(
        controller: controller,
        enabled: !_submitting,
        maxLength: 64,
        decoration: InputDecoration(labelText: label, counterText: ''),
        validator: _required,
      );
}
