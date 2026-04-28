# Swagger / OpenAPI API 명세

이 프로젝트는 `springdoc-openapi`로 서비스별 API 명세를 자동 생성합니다.

## 접속 URL

로컬 실행 기준:

| 서비스 | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Auth Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Menu Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Order Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| Review Service | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |

## 인증 사용법

1. `auth-service` Swagger에서 `POST /auth/login` 또는 `POST /auth/register`를 호출합니다.
2. 응답의 `accessToken` 값을 복사합니다.
3. 각 Swagger UI 오른쪽 상단의 `Authorize` 버튼을 누릅니다.
4. `Bearer` 접두사 없이 토큰 값만 입력합니다.

API Gateway를 통과하는 실제 클라이언트 요청은 `Authorization: Bearer <accessToken>` 헤더를 사용합니다. Gateway가 내부 서비스로 `X-User-Id`, `X-User-Email`, `X-User-Role` 헤더를 전달합니다.

## 관리자 권한 API

다음 API는 관리자 토큰이 필요합니다.

- 메뉴 등록, 수정, 삭제: `POST/PUT/DELETE /menus`
- 카테고리 등록, 삭제: `POST/DELETE /categories`
- 전체 주문 조회, 주문 상태 변경, 매출 통계: `/orders` 관리자 API
- 쿠폰 등록, 수정, 삭제, 목록 조회: `/coupons` 관리자 API
- 타 사용자 리뷰 삭제/수정: `/reviews/{id}` 관리자 권한

## 자동화 방식

- 컨트롤러 변경 사항은 서버 재시작 후 `/v3/api-docs`와 Swagger UI에 자동 반영됩니다.
- DTO의 Bean Validation 어노테이션은 스키마 제약으로 함께 노출됩니다.
- 서비스별 `OpenApiConfig`에서 문서 제목, 설명, JWT Bearer 인증 스키마, 로컬 서버 URL을 관리합니다.
