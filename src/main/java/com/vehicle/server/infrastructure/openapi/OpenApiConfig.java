package com.vehicle.server.infrastructure.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档的全局元数据配置。
 *
 * <p>springdoc 会基于 Controller 自动生成接口文档；此配置仅声明系统名称、版本和说明。</p>
 *
 * <p>bearerAuth 安全方案让 knife4j/Swagger UI 可以携带 JWT Token 调试受保护接口。</p>
 */
@OpenAPIDefinition(
        info = @Info(
                title = "车辆管理系统 API",
                version = "v1",
                description = "车辆管理系统后端接口文档",
                license = @License(name = "Internal")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Configuration
public class OpenApiConfig {
}
