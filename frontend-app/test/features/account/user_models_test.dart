import 'package:flutter_test/flutter_test.dart';
import 'package:shopping_app/features/account/domain/auth_models.dart';
import 'package:shopping_app/features/account/domain/user_models.dart';

void main() {
  test('解析用户资料响应', () {
    final profile = UserProfile.fromJson({
      'userId': 7,
      'nickname': 'Alice',
      'avatarUrl': null,
      'realName': '',
      'gender': 'FEMALE',
      'birthday': '2000-01-02',
      'bio': 'hello',
    });

    expect(profile.nickname, 'Alice');
    expect(profile.birthday, DateTime(2000, 1, 2));
  });

  test('解析收货地址和偏好响应', () {
    final address = UserAddress.fromJson({
      'id': 3,
      'recipientName': 'Alice',
      'recipientPhone': '13800000000',
      'provinceCode': null,
      'provinceName': '广东省',
      'cityCode': null,
      'cityName': '深圳市',
      'districtCode': null,
      'districtName': '南山区',
      'detailAddress': '测试路 1 号',
      'postalCode': null,
      'isDefault': true,
    });
    final preference = UserPreference.fromJson({
      'userId': 7,
      'marketingEnabled': false,
      'orderNotificationEnabled': true,
      'systemNotificationEnabled': true,
      'extraPreferences': {'theme': 'system'},
    });

    expect(address.isDefault, isTrue);
    expect(preference.extraPreferences['theme'], 'system');
  });

  test('解析登录设备响应并兼容字符串 ID', () {
    final device = DeviceSession.fromJson({
      'id': '201',
      'deviceType': 'ANDROID',
      'deviceName': 'Pixel',
      'appVersion': null,
      'maskedIp': '192.168.*.*',
      'lastActiveAt': '2026-08-19T18:00:00',
      'createdAt': '2026-08-10T09:00:00',
      'status': 'ACTIVE',
      'current': true,
      'sessionExpiresAt': '2026-08-26T18:00:00',
    });

    expect(device.id, '201');
    expect(device.current, isTrue);
    expect(device.sessionExpiresAt, isNotNull);
  });
}
