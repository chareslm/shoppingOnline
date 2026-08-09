# Shopping Backend

一个 Java 21 + Spring Boot 4.1 的模块化单体后端工程。

## 前置条件

- JDK 21。
- Maven 3.6.3 或更高版本。
- MySQL 8.4（首次实现数据库模块后需要）。

Spring Boot 4.1 支持 Java 21，MyBatis-Plus 使用对应的 Spring Boot 4 starter。[Spring Boot 系统要求](https://docs.spring.io/spring-boot/system-requirements.html) [MyBatis-Plus 安装指南](https://baomidou.com/en/getting-started/install/)

## 本地配置

复制 `src/main/resources/application-local.yml.example` 为 `application-local.yml`，填入本地 MySQL 地址和至少 32 字节的 `JWT_SECRET`。该本地配置被 Git 忽略。

也可以通过环境变量提供：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
REDIS_HOST
REDIS_PORT
ELASTICSEARCH_URIS
```

## 启动

```bash
mvn spring-boot:run
```

在未配置 MySQL 和 `JWT_SECRET` 前，应用会拒绝启动；这是为了避免以空数据库配置或弱密钥运行。

## 模块

根 package 是 `com.chareslm.shopping`。模块归属和 `common` 边界见项目管理员本地的 `docs/backend-package-architecture.md`。
