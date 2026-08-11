import 'package:shopping_app/core/network/api_response.dart';

class UserProfile {
  const UserProfile({
    required this.userId,
    required this.gender,
    this.nickname,
    this.avatarUrl,
    this.realName,
    this.birthday,
    this.bio,
  });

  factory UserProfile.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return UserProfile(
      userId: (json['userId'] as num).toInt(),
      nickname: json['nickname'] as String?,
      avatarUrl: json['avatarUrl'] as String?,
      realName: json['realName'] as String?,
      gender: json['gender']?.toString() ?? 'UNKNOWN',
      birthday: json['birthday'] == null
          ? null
          : DateTime.tryParse(json['birthday'].toString()),
      bio: json['bio'] as String?,
    );
  }

  final int userId;
  final String? nickname;
  final String? avatarUrl;
  final String? realName;
  final String gender;
  final DateTime? birthday;
  final String? bio;
}

class UserAddress {
  const UserAddress({
    required this.id,
    required this.recipientName,
    required this.recipientPhone,
    required this.provinceName,
    required this.cityName,
    required this.districtName,
    required this.detailAddress,
    required this.isDefault,
    this.provinceCode,
    this.cityCode,
    this.districtCode,
    this.postalCode,
  });

  factory UserAddress.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return UserAddress(
      id: (json['id'] as num).toInt(),
      recipientName: json['recipientName'] as String,
      recipientPhone: json['recipientPhone'] as String,
      provinceCode: json['provinceCode'] as String?,
      provinceName: json['provinceName'] as String,
      cityCode: json['cityCode'] as String?,
      cityName: json['cityName'] as String,
      districtCode: json['districtCode'] as String?,
      districtName: json['districtName'] as String,
      detailAddress: json['detailAddress'] as String,
      postalCode: json['postalCode'] as String?,
      isDefault: json['isDefault'] == true,
    );
  }

  final int id;
  final String recipientName;
  final String recipientPhone;
  final String? provinceCode;
  final String provinceName;
  final String? cityCode;
  final String cityName;
  final String? districtCode;
  final String districtName;
  final String detailAddress;
  final String? postalCode;
  final bool isDefault;
}

class SaveUserAddress {
  const SaveUserAddress({
    required this.recipientName,
    required this.recipientPhone,
    required this.provinceName,
    required this.cityName,
    required this.districtName,
    required this.detailAddress,
    required this.isDefault,
    this.provinceCode = '',
    this.cityCode = '',
    this.districtCode = '',
    this.postalCode = '',
  });

  final String recipientName;
  final String recipientPhone;
  final String provinceCode;
  final String provinceName;
  final String cityCode;
  final String cityName;
  final String districtCode;
  final String districtName;
  final String detailAddress;
  final String postalCode;
  final bool isDefault;

  Map<String, Object?> toJson() => {
    'recipientName': recipientName,
    'recipientPhone': recipientPhone,
    'provinceCode': provinceCode,
    'provinceName': provinceName,
    'cityCode': cityCode,
    'cityName': cityName,
    'districtCode': districtCode,
    'districtName': districtName,
    'detailAddress': detailAddress,
    'postalCode': postalCode,
    'isDefault': isDefault,
  };
}

class UserPreference {
  const UserPreference({
    required this.userId,
    required this.marketingEnabled,
    required this.orderNotificationEnabled,
    required this.systemNotificationEnabled,
    required this.extraPreferences,
  });

  factory UserPreference.fromJson(Object? value) {
    final json = requireJsonMap(value);
    final extra = json['extraPreferences'];
    return UserPreference(
      userId: (json['userId'] as num).toInt(),
      marketingEnabled: json['marketingEnabled'] == true,
      orderNotificationEnabled: json['orderNotificationEnabled'] == true,
      systemNotificationEnabled: json['systemNotificationEnabled'] == true,
      extraPreferences: extra is Map
          ? Map<String, Object?>.from(extra)
          : const {},
    );
  }

  final int userId;
  final bool marketingEnabled;
  final bool orderNotificationEnabled;
  final bool systemNotificationEnabled;
  final Map<String, Object?> extraPreferences;
}
