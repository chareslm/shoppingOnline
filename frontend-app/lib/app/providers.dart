import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shopping_app/core/auth/session_controller.dart';
import 'package:shopping_app/core/config/app_config.dart';
import 'package:shopping_app/core/device/device_identity.dart';
import 'package:shopping_app/core/network/api_client.dart';
import 'package:shopping_app/core/storage/session_store.dart';
import 'package:shopping_app/features/account/data/auth_api.dart';
import 'package:shopping_app/features/account/data/auth_repository.dart';
import 'package:shopping_app/features/account/data/user_api.dart';
import 'package:shopping_app/features/account/data/user_repository.dart';
import 'package:shopping_app/features/account/data/statistics_api.dart';
import 'package:shopping_app/features/merchant/data/merchant_api.dart';
import 'package:shopping_app/features/message/data/message_api.dart';
import 'package:shopping_app/features/product/data/product_api.dart';
import 'package:shopping_app/features/trade/data/trade_api.dart';

final secureStorageProvider = Provider<FlutterSecureStorage>(
  (ref) => const FlutterSecureStorage(aOptions: AndroidOptions()),
);

final sessionStoreProvider = Provider<SessionStore>(
  (ref) => SecureSessionStore(ref.watch(secureStorageProvider)),
);

final authSessionProvider = ChangeNotifierProvider<AuthSessionController>(
  (ref) => AuthSessionController(ref.watch(sessionStoreProvider)),
);

final deviceIdentityProvider = Provider<DeviceIdentity>(
  (ref) => DeviceIdentity(ref.watch(secureStorageProvider)),
);

final apiClientProvider = Provider<ApiClient>(
  (ref) => ApiClient(AppConfig.apiBaseUrl, ref.read(authSessionProvider)),
);

final authApiProvider = Provider<AuthApi>(
  (ref) => AuthApi(ref.watch(apiClientProvider)),
);

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(
    ref.watch(authApiProvider),
    ref.read(authSessionProvider),
    ref.watch(deviceIdentityProvider),
  ),
);

final userApiProvider = Provider<UserApi>(
  (ref) => UserApi(ref.watch(apiClientProvider)),
);

final userRepositoryProvider = Provider<UserRepository>(
  (ref) => UserRepository(ref.watch(userApiProvider)),
);

final statisticsApiProvider = Provider<StatisticsApi>(
  (ref) => StatisticsApi(ref.watch(apiClientProvider)),
);

final productApiProvider = Provider<ProductApi>(
  (ref) => ProductApi(ref.watch(apiClientProvider)),
);

final tradeApiProvider = Provider<TradeApi>(
  (ref) => TradeApi(ref.watch(apiClientProvider)),
);

final merchantApiProvider = Provider<MerchantApi>(
  (ref) => MerchantApi(ref.watch(apiClientProvider)),
);

final messageApiProvider = Provider<MessageApi>(
  (ref) => MessageApi(ref.watch(apiClientProvider)),
);
