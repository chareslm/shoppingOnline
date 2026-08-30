import 'package:shopping_app/core/network/api_response.dart';

class ChatSession {
  const ChatSession({
    required this.sessionId,
    required this.userId,
    required this.status,
    required this.priority,
    required this.unreadCount,
    required this.createdAt,
    this.shopId,
    this.csUserId,
    this.subject,
    this.lastMessage,
    this.lastMessageTime,
  });

  factory ChatSession.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ChatSession(
      sessionId: json['sessionId'].toString(),
      userId: json['userId'].toString(),
      shopId: json['shopId']?.toString(),
      csUserId: json['csUserId']?.toString(),
      subject: json['subject'] as String?,
      lastMessage: json['lastMessage'] as String?,
      lastMessageTime: json['lastMessageTime'] as String?,
      status: readInt(json['status']),
      priority: readInt(json['priority']),
      unreadCount: readInt(json['unreadCount']),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  final String sessionId;
  final String userId;
  final String? shopId;
  final String? csUserId;
  final String? subject;
  final String? lastMessage;
  final String? lastMessageTime;
  final int status;
  final int priority;
  final int unreadCount;
  final String createdAt;
}

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.sessionId,
    required this.senderId,
    required this.senderType,
    required this.content,
    required this.msgType,
    required this.isRead,
    required this.status,
    required this.createdAt,
    this.senderName,
    this.senderAvatar,
    this.extraData,
    this.readTime,
  });

  factory ChatMessage.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return ChatMessage(
      id: json['id'].toString(),
      sessionId: json['sessionId'].toString(),
      senderId: json['senderId'].toString(),
      senderType: readInt(json['senderType']),
      senderName: json['senderName'] as String?,
      senderAvatar: json['senderAvatar'] as String?,
      content: json['content']?.toString() ?? '',
      msgType: readInt(json['msgType'], fallback: 1),
      extraData: json['extraData'] as String?,
      isRead: readInt(json['isRead']),
      readTime: json['readTime'] as String?,
      status: readInt(json['status']),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  final String id;
  final String sessionId;
  final String senderId;
  final int senderType;
  final String? senderName;
  final String? senderAvatar;
  final String content;
  final int msgType;
  final String? extraData;
  final int isRead;
  final String? readTime;
  final int status;
  final String createdAt;
}

class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.templateId,
    required this.templateCode,
    required this.title,
    required this.content,
    required this.category,
    required this.categoryDesc,
    required this.isRead,
    required this.pushStatus,
    required this.createdAt,
    this.bizType,
    this.bizId,
    this.readTime,
    this.pushTime,
  });

  factory NotificationItem.fromJson(Object? value) {
    final json = requireJsonMap(value);
    return NotificationItem(
      id: json['id'].toString(),
      templateId: json['templateId'].toString(),
      templateCode: json['templateCode']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      category: readInt(json['category']),
      categoryDesc: json['categoryDesc']?.toString() ?? '',
      bizType: json['bizType'] as String?,
      bizId: json['bizId']?.toString(),
      isRead: readInt(json['isRead']),
      readTime: json['readTime'] as String?,
      pushStatus: readInt(json['pushStatus']),
      pushTime: json['pushTime'] as String?,
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  final String id;
  final String templateId;
  final String templateCode;
  final String title;
  final String content;
  final int category;
  final String categoryDesc;
  final String? bizType;
  final String? bizId;
  final int isRead;
  final String? readTime;
  final int pushStatus;
  final String? pushTime;
  final String createdAt;
}

const sessionStatusLabels = {0: '进行中', 1: '已结束'};
