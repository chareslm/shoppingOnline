package com.chareslm.shopping.common.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration
public class JacksonConfig {

    /**
     * 供业务代码注入使用的 Jackson 2 ObjectMapper（用户偏好 extraPreferences、
     * 安全错误响应等内部 JSON 处理，不走 HTTP 消息转换器）。
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    /**
     * Spring MVC 实际使用 Jackson 3（Spring Boot 4）JsonMapper。
     * 雪花 ID（19 位）超出前端 JS Number 安全整数范围（2^53），
     * 全局将 Long 序列化为字符串避免精度丢失；金额（BigDecimal）保持数字。
     */
    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.addModule(new JacksonModule() {
            @Override
            public String getModuleName() {
                return "long-to-string";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.addSerializers(new Serializers() {
                    @Override
                    public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                                                             BeanDescription.Supplier beanDesc, JsonFormat.Value format) {
                        if (type.hasRawClass(Long.class) || type.hasRawClass(Long.TYPE)) {
                            return ToStringSerializer.instance;
                        }
                        return null;
                    }
                });
            }
        });
    }
}
