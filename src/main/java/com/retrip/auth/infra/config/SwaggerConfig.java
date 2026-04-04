package com.retrip.auth.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .components(
                        new Components()
                                .addSecuritySchemes("BearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("로그인/소셜 로그인 후 발급된 Access Token을 입력하세요. (Bearer 접두사 불필요)")
                                )
                )
                .info(
                        new Info()
                                .title("ReTrip Auth API")
                                .version("v0.0.1")
                                .description("ReTrip 인증 서비스 API 명세\n\n" +
                                        "## 인증 방식\n" +
                                        "- **이메일 로그인**: `POST /login` → Access Token 발급\n" +
                                        "- **소셜 로그인**: 브라우저에서 `GET /oauth2/authorization/{provider}` → 콜백 URL로 Token 전달\n" +
                                        "- **Refresh Token**: HttpOnly Cookie 방식 (자동 전송)\n\n" +
                                        "## ⚠️ 프론트엔드 필수 설정\n" +
                                        "### withCredentials 설정 (쿠키 전송)\n" +
                                        "`/login` · `/auth/reissue` · `/auth/logout` 은 HttpOnly 쿠키(refreshToken)를 사용합니다.\n" +
                                        "**크로스 오리진 환경에서 반드시 아래 설정이 필요합니다.**\n" +
                                        "- axios: `withCredentials: true`\n" +
                                        "- fetch: `credentials: 'include'`\n\n" +
                                        "이 설정이 없으면 쿠키가 전송되지 않아 **토큰 재발급과 로그아웃이 동작하지 않습니다.**\n\n" +
                                        "### refreshToken 저장 금지\n" +
                                        "로그인 응답 body에 refreshToken이 포함되더라도 **localStorage/sessionStorage에 저장하지 마세요.**\n" +
                                        "서버가 Set-Cookie로 자동 관리합니다. 프론트는 **accessToken만** 저장하세요.\n\n" +
                                        "## 주요 특이사항\n" +
                                        "- `/login` 요청 시 이메일 필드명은 `email`이 아닌 **`id`** 입니다.\n" +
                                        "- 소셜 전용 계정은 `hasPassword=false`. PATCH /users/password → 403, POST /users/password로 최초 설정 가능.\n" +
                                        "- 본인인증 완료 후(`isVerified=true`) name/birthDate 변경 불가.\n" +
                                        "- `travelStyles` 필드는 ID가 아닌 **이름 문자열 배열**입니다. (예: `[\"계획철저\", \"사진광\"]`)")
                )
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                // Spring Security 필터 레벨 엔드포인트 수동 등록
                .path("/login", loginPathItem())
                .path("/oauth2/authorization/{provider}", oauthPathItem())
                .path("/auth/reissue", reissuePathItem())
                .path("/auth/logout", logoutPathItem());
    }

    private PathItem loginPathItem() {
        Schema<?> requestSchema = new ObjectSchema()
                .addProperty("id", new StringSchema()
                        .example("user@example.com")
                        .description("이메일 주소 (필드명 주의: email이 아닌 id)"))
                .addProperty("password", new StringSchema()
                        .example("Test1234!")
                        .description("비밀번호"))
                .required(List.of("id", "password"));

        Schema<?> responseSchema = new ObjectSchema()
                .addProperty("accessToken", new StringSchema().description("JWT Access Token (30분 유효)"))
                .addProperty("tokenType", new StringSchema().example("Bearer"))
                .addProperty("expiresIn", new Schema<Integer>().example(1800).description("만료 시간(초)"));

        return new PathItem().post(
                new Operation()
                        .tags(List.of("인증 (Security Filter)"))
                        .summary("이메일 로그인")
                        .description(
                                "Spring Security LoginAuthenticationFilter가 처리합니다. 컨트롤러 코드 없음.\n\n" +
                                "**주의**: 요청 본문의 이메일 필드명이 `email`이 아닌 **`id`** 입니다.\n\n" +
                                "성공 시 Response Body에 accessToken, Set-Cookie 헤더에 refreshToken(HttpOnly) 발급.")
                        .requestBody(new RequestBody()
                                .required(true)
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(requestSchema))))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("로그인 성공 — accessToken 반환 + Set-Cookie: refreshToken")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(responseSchema))))
                                .addApiResponse("401", new ApiResponse()
                                        .description("로그인 실패 — 비밀번호 불일치(Member-002) 또는 회원 없음(Member-001)")))
                        .security(List.of()) // 이 엔드포인트는 인증 불필요
        );
    }

    private PathItem oauthPathItem() {
        Parameter providerParam = new Parameter()
                .in("path")
                .name("provider")
                .required(true)
                .description("소셜 로그인 제공자")
                .schema(new StringSchema()
                        ._enum(List.of("kakao", "naver", "google"))
                        .example("kakao"));

        return new PathItem().get(
                new Operation()
                        .tags(List.of("인증 (Security Filter)"))
                        .summary("소셜 로그인 시작 (브라우저 전용)")
                        .description(
                                "Spring Security OAuth2 필터가 처리합니다. **브라우저에서만 동작합니다.**\n\n" +
                                "**플로우**:\n" +
                                "1. 브라우저에서 이 URL로 접속\n" +
                                "2. 소셜 서비스 인증 페이지로 리다이렉트\n" +
                                "3. 인증 완료 후 `{FRONTEND_CALLBACK_URL}?accessToken=...&refreshToken=...` 으로 최종 리다이렉트\n\n" +
                                "**콜백 URL**: `http://localhost:3000/auth/callback` (로컬), `https://retrip.io/auth/callback` (운영)\n\n" +
                                "⚠️ Swagger UI의 Try it out으로는 직접 실행 불가 (리다이렉트 방식).")
                        .addParametersItem(providerParam)
                        .responses(new ApiResponses()
                                .addApiResponse("302", new ApiResponse()
                                        .description("소셜 인증 페이지로 리다이렉트")))
                        .security(List.of()) // 인증 불필요
        );
    }

    private PathItem reissuePathItem() {
        Schema<?> responseSchema = new ObjectSchema()
                .addProperty("accessToken", new StringSchema().description("새로 발급된 JWT Access Token"));

        return new PathItem().post(
                new Operation()
                        .tags(List.of("인증 (Security Filter)"))
                        .summary("Access Token 재발급 (RTR)")
                        .description(
                                "HttpOnly Cookie의 refreshToken으로 새 accessToken을 발급합니다.\n\n" +
                                "**Refresh Token Rotation(RTR)**: 재발급 시 기존 refreshToken은 폐기되고 새 refreshToken이 Cookie로 발급됩니다.\n\n" +
                                "Swagger에서 호출 시 쿠키가 자동 전송되지 않을 수 있습니다. test.html을 사용하세요.")
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("재발급 성공")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(responseSchema))))
                                .addApiResponse("401", new ApiResponse()
                                        .description("refreshToken 만료 또는 없음")))
                        .security(List.of())
        );
    }

    private PathItem logoutPathItem() {
        return new PathItem().post(
                new Operation()
                        .tags(List.of("인증 (Security Filter)"))
                        .summary("로그아웃")
                        .description(
                                "서버에 저장된 refreshToken을 삭제하고 Cookie를 만료 처리합니다.\n\n" +
                                "클라이언트는 저장된 accessToken도 삭제해야 합니다.")
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("로그아웃 성공")))
        );
    }
}
