import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/network/api_response.dart';
import 'package:shopping_app/features/message/domain/message_models.dart';

class MessageApi {
  MessageApi(this._client);

  final ApiClient _client;

  Future<ChatSession> createSession({
    String? shopId,
    String? subject,
    String? firstMessage,
  }) async {
    final response = await _client.post(
      '/api/chat/sessions',
      data: {
        if (shopId != null && shopId.isNotEmpty) 'shopId': shopId,
        if (subject != null && subject.isNotEmpty) 'subject': subject,
        if (firstMessage != null && firstMessage.isNotEmpty)
          'firstMessage': firstMessage,
      },
    );
    return unwrapApiResponse(response.data, ChatSession.fromJson);
  }

  Future<List<ChatSession>> listMySessions() async {
    final response = await _client.get('/api/chat/sessions');
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, ChatSession.fromJson, message: '会话列表格式错误'),
    );
  }

  Future<List<ChatSession>> listCsSessions() async {
    final response = await _client.get('/api/chat/sessions/cs');
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, ChatSession.fromJson, message: '客服会话列表格式错误'),
    );
  }

  Future<ChatSession> assignSession(String sessionId) async {
    final response = await _client.put('/api/chat/sessions/$sessionId/assign');
    return unwrapApiResponse(response.data, ChatSession.fromJson);
  }

  Future<void> closeSession(String sessionId) async {
    final response = await _client.put('/api/chat/sessions/$sessionId/close');
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<List<ChatMessage>> listMessages(
    String sessionId, {
    int page = 1,
    int pageSize = 50,
  }) async {
    final response = await _client.get(
      '/api/chat/messages/$sessionId',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return unwrapApiResponse(
      response.data,
      (value) => readObjectList(value, ChatMessage.fromJson, message: '消息列表格式错误'),
    );
  }

  Future<ChatMessage> sendMessage(String sessionId, String content) async {
    final response = await _client.post(
      '/api/chat/messages/$sessionId',
      data: {'content': content, 'msgType': 1},
    );
    return unwrapApiResponse(response.data, ChatMessage.fromJson);
  }

  Future<List<NotificationItem>> listNotifications({
    int page = 1,
    int pageSize = 20,
  }) async {
    final response = await _client.get(
      '/api/message/notifications',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return unwrapApiResponse(
      response.data,
      (value) =>
          readObjectList(value, NotificationItem.fromJson, message: '通知列表格式错误'),
    );
  }

  Future<void> markNotificationRead(String notificationId) async {
    final response = await _client.put(
      '/api/message/notifications/$notificationId/read',
    );
    unwrapApiResponse<void>(response.data, (_) {});
  }

  Future<void> markAllNotificationsRead() async {
    final response = await _client.put('/api/message/notifications/read-all');
    unwrapApiResponse<void>(response.data, (_) {});
  }
}
