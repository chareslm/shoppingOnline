import 'package:flutter_test/flutter_test.dart';
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
}
