import 'package:shopping_app/core/network/api_response.dart';

class MerchantApplicationRequest {
  const MerchantApplicationRequest({
    required this.merchantType,
    required this.shopName,
    required this.responsiblePersonName,
    required this.identityDocumentType,
    required this.identityDocumentNumber,
    required this.contactPhone,
    required this.contactEmail,
    this.subjectName,
    this.unifiedSocialCreditCode,
  });

  final String merchantType;
  final String shopName;
  final String? subjectName;
  final String? unifiedSocialCreditCode;
  final String responsiblePersonName;
  final String identityDocumentType;
  final String identityDocumentNumber;
  final String contactPhone;
  final String contactEmail;

  Map<String, Object?> toJson() => {
    'merchantType': merchantType,
    'shopName': shopName,
    if (subjectName != null) 'subjectName': subjectName,
    if (unifiedSocialCreditCode != null)
      'unifiedSocialCreditCode': unifiedSocialCreditCode,
    'responsiblePersonName': responsiblePersonName,
    'identityDocumentType': identityDocumentType,
    'identityDocumentNumber': identityDocumentNumber,
    'contactPhone': contactPhone,
    'contactEmail': contactEmail,
  };
}

class MerchantApplicationReceipt {
  const MerchantApplicationReceipt({required this.id, required this.status});

  factory MerchantApplicationReceipt.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return MerchantApplicationReceipt(
      id: json['id'].toString(),
      status: json['status']?.toString() ?? '',
    );
  }

  final String id;
  final String status;
}

class ShopSummary {
  const ShopSummary({
    required this.id,
    required this.name,
    required this.status,
  });

  factory ShopSummary.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ShopSummary(
      id: json['id'].toString(),
      name: json['name']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
    );
  }

  final String id;
  final String name;
  final String status;
}

class ShopStaffAccount {
  const ShopStaffAccount({
    required this.id,
    required this.shopId,
    required this.userId,
    required this.displayName,
    required this.status,
    required this.emailDeliveryStatus,
    required this.mustChangePassword,
    required this.createdAt,
    this.shopName,
    this.maskedEmail,
    this.username,
    this.auditRemark,
  });

  factory ShopStaffAccount.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ShopStaffAccount(
      id: json['id'].toString(),
      shopId: json['shopId'].toString(),
      shopName: json['shopName'] as String?,
      userId: json['userId'].toString(),
      displayName: json['displayName']?.toString() ?? '',
      maskedEmail: json['maskedEmail'] as String?,
      username: json['username'] as String?,
      status: json['status']?.toString() ?? '',
      auditRemark: json['auditRemark'] as String?,
      emailDeliveryStatus: json['emailDeliveryStatus']?.toString() ?? '',
      mustChangePassword: json['mustChangePassword'] == true,
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  final String id;
  final String shopId;
  final String? shopName;
  final String userId;
  final String displayName;
  final String? maskedEmail;
  final String? username;
  final String status;
  final String? auditRemark;
  final String emailDeliveryStatus;
  final bool mustChangePassword;
  final String createdAt;
}

class ShopStatisticsOverview {
  const ShopStatisticsOverview({
    required this.metricVersion,
    required this.timezone,
    required this.shopName,
    required this.metrics,
  });

  factory ShopStatisticsOverview.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ShopStatisticsOverview(
      metricVersion: json['metricVersion']?.toString() ?? '',
      timezone: json['timezone']?.toString() ?? '',
      shopName: json['shopName']?.toString() ?? '',
      metrics: ShopMetrics.fromJson(json['metrics']),
    );
  }

  final String metricVersion;
  final String timezone;
  final String shopName;
  final ShopMetrics metrics;
}

class ShopMetrics {
  const ShopMetrics({
    required this.paidOrderCount,
    required this.paidBuyerCount,
    required this.grossPaidAmount,
    required this.successfulRefundAmount,
    required this.netCashflowActivity,
    required this.soldQuantity,
    required this.onSaleProductSnapshot,
    required this.displayedReviewCount,
    this.averageOrderValue,
    this.averageRating,
  });

  factory ShopMetrics.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ShopMetrics(
      paidOrderCount: json['paidOrderCount']?.toString() ?? '0',
      paidBuyerCount: json['paidBuyerCount']?.toString() ?? '0',
      grossPaidAmount: json['grossPaidAmount']?.toString() ?? '0',
      successfulRefundAmount: json['successfulRefundAmount']?.toString() ?? '0',
      netCashflowActivity: json['netCashflowActivity']?.toString() ?? '0',
      averageOrderValue: json['averageOrderValue']?.toString(),
      soldQuantity: json['soldQuantity']?.toString() ?? '0',
      onSaleProductSnapshot: json['onSaleProductSnapshot']?.toString() ?? '0',
      displayedReviewCount: json['displayedReviewCount']?.toString() ?? '0',
      averageRating: json['averageRating']?.toString(),
    );
  }

  final String paidOrderCount;
  final String paidBuyerCount;
  final String grossPaidAmount;
  final String successfulRefundAmount;
  final String netCashflowActivity;
  final String? averageOrderValue;
  final String soldQuantity;
  final String onSaleProductSnapshot;
  final String displayedReviewCount;
  final String? averageRating;
}

const staffStatusLabels = {
  'PENDING_AUDIT': '待审核',
  'ACTIVE': '已开通',
  'REJECTED': '已驳回',
  'REVOKED': '已撤销',
  'DISABLED': '已停用',
};
