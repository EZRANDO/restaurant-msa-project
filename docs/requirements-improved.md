## MSA 기반 식당 주문 서비스 — 요구사항 명세서 (수정·첨삭)

### 1. 프로젝트 개요

MSA 아키텍처 기반 온라인 식당 주문 서비스로, 고객용/관리자용 웹을 분리 제공합니다.  
AI 기능(메뉴 추천, 리뷰 요약)을 통한 고급 기능을 제공하며, 각 마이크로서비스는 Docker 컨테이너로 독립 배포되어 확장성과 유지보수성을 보장합니다.

**시스템 특징:**
- 고객: 메뉴 검색/주문/리뷰 작성에 최적화
- 관리자: 메뉴/주문/통계 관리 중심
- AI 기반: 자연어 검색, 음식 추천, 리뷰 요약
- 확장성: 마이크로서비스별 독립 배포 및 확장 가능

---

### 2. 기술 스택

| 구분 | 기술 | 상세 | 버전 |
| --- | --- | --- | --- |
| **프론트엔드** | Vue 3 + Vite | 고객용/관리자용 별도 빌드 | 최신 |
| **백엔드** | Java 17+ | Spring Boot | 3.0+ |
| **데이터베이스** | SQLite3 | 각 마이크로서비스별 독립 DB | 최신 |
| **캐시/세션** | Redis | Refresh Token, JWT Blacklist, 세션 관리 | 7-alpine |
| **AI 서비스** | Python 3.10+ | FastAPI 프레임워크 | 0.100+ |
| **AI 모델** | OpenAI GPT API | 메뉴 추천, 리뷰 요약 | gpt-3.5-turbo+ |
| **인증** | JWT | Access Token + Refresh Token | - |
| **배포** | Docker + Docker Compose | 각 서비스별 Dockerfile | 최신 |
| **아키텍처** | MSA + API Gateway | 모든 외부 요청 API Gateway 통한 통일 라우팅 | - |

---

### 3. 시스템 아키텍처

#### 3.1 마이크로서비스 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                        클라이언트 (외부)                          │
│          frontend-customer (3000)  frontend-admin (3001)         │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                 ┌───────▼────────┐
                 │  API Gateway   │ (8088)
                 │  - JWT 검증    │
                 │  - 라우팅      │
                 └───────┬────────┘
                         │
        ┌────────────────┼─────────────────┬──────────────────┐
        │                │                  │                  │
   ┌────▼────┐    ┌─────▼─────┐    ┌──────▼──────┐    ┌─────▼────┐
   │ auth-   │    │   menu-   │    │   order-    │    │ review-  │
   │ service │    │  service  │    │   service   │    │ service  │
   │(8081)   │    │  (8082)   │    │  (8083)     │    │ (8084)   │
   │         │    │           │    │             │    │          │
   │SQLite3  │    │ SQLite3   │    │  SQLite3    │    │ SQLite3  │
   └────┬────┘    └─────┬─────┘    └──────┬──────┘    └─────┬────┘
        │                │                │                  │
        └────────────────┼─────────────────┴──────────────────┘
                         │
                 ┌───────▼────────┐
                 │  AI Service    │ (8085)
                 │  (Python       │
                 │   FastAPI)     │
                 │- 음식 추천     │
                 │- 리뷰 요약     │
                 └────────────────┘
                         │
        ┌────────────────┴──────────────────┐
        │                                   │
   ┌────▼────┐                      ┌──────▼──────┐
   │  Redis  │                      │ OpenAI GPT  │
   │ (내부)  │                      │ API (외부)  │
   └─────────┘                      └─────────────┘

모든 서비스는 Docker msa-net 내부 네트워크를 통해 통신
```

#### 3.2 마이크로서비스 상세 설명

| 서비스 | 포트 | 책임 | 기술 | 데이터베이스 |
| --- | --- | --- | --- | --- |
| **api-gateway** | 8088 | 모든 외부 요청 수신, JWT 검증, 인증 실패 시 401 반환, 요청 라우팅 | Spring Boot | - |
| **auth-service** | 8081 | 회원가입/로그인, Access/Refresh Token 발급/갱신, 로그아웃 처리, Redis Blacklist 관리 | Spring Boot | SQLite3 |
| **menu-service** | 8082 | 메뉴 CRUD, 카테고리 관리, 메뉴 검색(name, category, 자연어 기반) | Spring Boot | SQLite3 |
| **order-service** | 8083 | 주문 생성/조회, 주문 상태 관리, 정산 처리, 쿠폰 적용, 통계 데이터 제공 | Spring Boot | SQLite3 |
| **review-service** | 8084 | 리뷰 CRUD, 별점 관리, 메뉴별 리뷰 조회, AI 요약용 데이터 제공 | Spring Boot | SQLite3 |
| **ai-service** | 8085 | 음식 추천 (시간대별), 리뷰 요약 (자동 생성) | Python + FastAPI | SQLite3 |
| **redis** | 6379(내부) | Refresh Token 저장, JWT Blacklist 관리, 세션 캐싱 | Redis 7 | 메모리 기반 |

#### 3.3 서비스 간 통신

- **HTTP REST API**: 모든 서비스 간 동기식 통신은 HTTP 기반 REST API 사용
- **내부 네트워크 접근**: 모든 백엔드 서비스는 Docker msa-net 내부 네트워크에서만 접근 가능
  - auth-service → Redis (Refresh Token/Blacklist)
  - order-service ← ai-service (주문 데이터 조회)
  - review-service ← ai-service (리뷰 데이터 조회)
- **외부 API**: ai-service ↔ OpenAI GPT API (외부 인터넷 필요)
- **라우팅**: 모든 외부 요청 → API Gateway → 각 서비스로 라우팅

---

### 4. 기능 요구사항

#### 4.1 고객용 웹 (frontend-customer, 포트: 3000)

**4.1.1 메인 화면**
- 상단 고정 네비게이션바
  - 로고 / 홈 버튼
  - 로그인/로그아웃 상태 표시
  - 마이페이지 버튼 (로그인 시에만 표시)
  - 장바구니 아이콘 (담은 상품 수 표시)

- 키워드 검색바 (AI 기반 자연어 검색)
  - 예: "다이어트 음식", "매운 음식", "빠른 음식", "저렴한 음식" → AI가 해석하여 메뉴 필터링
  - 검색 결과는 실시간으로 업데이트

- 카테고리 메뉴바 (한식, 중식, 일식, 양식, 분식, 해산물 등)
  - 클릭 시 해당 카테고리 메뉴 조회
  - 멀티 선택 가능 (여러 카테고리 동시 필터링)

- AI 추천 섹션 ("이런 음식은 어때요?")
  - 시간대 기반 추천: "점심엔 ○○ 메뉴 어때요?", "저녁엔 ○○ 메뉴 어때요?"
  - 최근 주문 집계 데이터 기반
  - 회전 배너 형식으로 표시 (자동 갱신: 5분 마다)

**4.1.2 메뉴 검색 및 조회**
- 자연어 키워드 기반 검색
  - 입력값: "다이어트", "매운", "빠른", "저렴" 등
  - 메뉴명, 설명에 포함된 메뉴 자동 필터링
  - 검색 결과 표시 (메뉴명, 이미지, 가격, 별점)

- 카테고리 필터
  - 단일 선택 또는 다중 선택
  - 가격대별 필터링 (예: 5,000~10,000원, 10,000~20,000원 등)
  - 평점순, 신상품순, 인기순 정렬

- 메뉴 상세 조회
  - 메뉴명, 이미지, 상세 설명, 가격
  - 최근 리뷰 3개 표시
  - 평균 별점, 리뷰 수

**4.1.3 장바구니**
- 메뉴 담기 (수량 선택 가능)
- 장바구니 메뉴 목록 조회
  - 메뉴명, 가격, 수량, 소계 표시
- 수량 조절 (증감 버튼)
- 항목 삭제
- 합계 금액 실시간 계산 및 표시
- 할인 쿠폰 코드 입력 필드
  - 유효 쿠폰 적용 시 할인금액 표시
  - 최소 주문 금액 미충족 시 경고 메시지

**4.1.4 주문**
- 주문 생성
  - 장바구니 기반 주문 생성
  - 배송지 입력 (선택사항: 배송/픽업)
  - 특별 요청사항 입력 (예: 맵지 않게, 소금 적게 등)
  - 결제 수단 선택 (체험용: 결제 모의 진행)

- 쿠폰 자동 적용
  - 유효한 쿠폰이 있으면 자동 적용
  - 최소 주문 금액 충족 여부 확인
  - 할인 유형: 고정금액 또는 비율(%)

- 주문 목록 조회
  - 상태별 조회: 대기 중 / 준비 중 / 완료
  - 주문번호, 주문 날짜, 메뉴명, 합계 금액, 상태 표시
  - 각 주문 클릭 시 상세 조회 가능

- 주문 취소
  - 대기 중 상태에서만 취소 가능
  - 취소 사유 입력 (선택사항)

**4.1.5 리뷰**
- 리뷰 작성
  - 완료된 주문에 대해서만 리뷰 작성 가능
  - 별점(1~5) + 코멘트 (최소 10자, 최대 500자)
  - 이미지 첨부 가능 (선택사항)
  - 작성 후 즉시 리뷰 목록에 반영

- 본인 리뷰 관리
  - 작성한 리뷰 목록 조회
  - 리뷰 수정 (작성 후 30일 이내)
  - 리뷰 삭제

- 메뉴별 리뷰 조회
  - 메뉴 상세 페이지에서 전체 리뷰 조회
  - 최신순, 평점순, 도움순 정렬
  - 댓글 기능 (선택사항)

**4.1.6 인증 및 사용자 정보**
- 회원가입
  - 이메일, 비밀번호, 이름, 전화번호 입력
  - 이메일 중복 확인
  - 비밀번호 유효성 검사 (8자 이상, 숫자+문자 포함)

- 로그인
  - 이메일 + 비밀번호
  - JWT Access Token 받음 (30분 유효)
  - Refresh Token은 httpOnly Cookie에 저장

- 로그아웃
  - 로그아웃 시 Refresh Token Redis Blacklist에 등록
  - 세션 정리

- 마이페이지
  - 회원 정보 조회/수정
  - 비밀번호 변경
  - 주문 이력 조회

---

#### 4.2 관리자용 웹 (frontend-admin, 포트: 3001)

**4.2.1 대시보드**
- 오늘 매출, 이번 달 매출, 누적 매출 요약
- 최근 주문 3개 카드 (주문번호, 금액, 상태)
- 인기 메뉴 TOP 5 (판매량 기준)
- 최근 리뷰 3개 (평점, 코멘트)
- 시간대별 주문 건수 추이 (차트)

**4.2.2 메뉴 관리**
- 메뉴 CRUD
  - 메뉴명 (필수)
  - 카테고리 (필수, 드롭다운)
  - 상세 설명 (필수, textarea)
  - 가격 (필수, 숫자)
  - 이미지 (필수, 이미지 업로드)
  - 노출 여부 (체크박스)
  - 생성/수정 일시 자동 기록

- 메뉴 목록 조회
  - 테이블: 메뉴명, 카테고리, 가격, 리뷰 수, 평균 별점, 노출 여부, 작업 버튼
  - 검색 (메뉴명, 카테고리)
  - 페이지네이션 (10개 단위)

- 카테고리 관리 (선택사항)
  - 카테고리 추가/수정/삭제
  - 상위/하위 카테고리 계층 구조 지원

**4.2.3 주문 관리**
- 신규 주문 목록 (상태: PENDING)
  - 주문번호, 고객명, 메뉴명, 금액, 특별 요청사항, 수신 시간
  - 알림: 미확인 주문 개수 표시
  - 알람음 또는 시각적 알림 (선택사항)

- 주문 상태 변경
  - 상태 흐름: PENDING → PREPARING → DONE
  - 버튼 클릭으로 상태 변경
  - 변경 시간 자동 기록

- 주문 목록 조회
  - 필터: 상태(전체/대기/준비/완료), 날짜 범위
  - 정렬: 최신순, 금액순
  - 페이지네이션

- 주문 상세 조회
  - 고객 정보, 주문 항목, 결제 정보, 배송지, 주문 진행 상황

**4.2.4 매출/통계**
- 기간별 매출 집계
  - 일별 매출 (날짜별 표 또는 차트)
  - 월별 매출 (연도 선택 가능)
  - 연도별 매출

- 날짜 범위 조회
  - 시작일 ~ 종료일 선택
  - 해당 기간 매출, 주문 건수, 평균 주문 금액 표시

- 인기 메뉴 분석
  - TOP 10 메뉴 (판매량 기준)
  - 메뉴명, 판매량, 매출액, 평균 별점
  - 차트 또는 테이블 형식

- 카테고리별 매출 분석 (선택사항)
  - 카테고리별 판매 비율 (원형 차트)
  - 시간대별 판매 추이

**4.2.5 후기 관리**
- 전체 리뷰 조회
  - 테이블: 리뷰어, 메뉴명, 별점, 코멘트(요약), 작성일
  - 검색: 메뉴명, 리뷰어
  - 별점 필터 (1~5점)
  - 페이지네이션

- 리뷰 삭제
  - 부적절한 리뷰 삭제 가능
  - 삭제 전 확인 메시지

- **AI 리뷰 요약** (자동 생성)
  - 페이지 상단에 전체 리뷰 분석 결과 표시
  - 형식: "고객님들께서 [메뉴명]을(를) 매우 만족하시며, 특히 [맛/가성비/신속성] 측면에서 호평입니다. 다만 [개선 사항]이(가) 있습니다." (3~5문장)
  - 갱신 빈도: 매일 자정 또는 수동 갱신 버튼

**4.2.6 쿠폰 관리**
- 쿠폰 CRUD
  - 쿠폰 코드 (필수, 예: SUMMER2024, SAVE10)
  - 할인 유형: 고정금액 또는 비율(%)
  - 할인값 (필수)
  - 최소 주문 금액 (선택, 예: 10,000원 이상 시만 사용 가능)
  - 유효 기간 (시작일 ~ 종료일)
  - 사용 제한: 1인당 사용 횟수 제한 (예: 1회만)
  - 활성화 여부

- 쿠폰 목록 조회
  - 테이블: 코드, 할인, 최소금액, 유효기간, 사용 현황, 상태
  - 유효 쿠폰 / 만료 쿠폰 분류
  - 페이지네이션

- 쿠폰 통계 (선택사항)
  - 각 쿠폰별 사용 횟수
  - 총 할인액

---

### 5. 백엔드 요구사항

#### 5.1 인증·인가

**JWT 토큰 관리**
- **Access Token**
  - 만료 시간: 30분 (설정 가능)
  - 전달 방식: HTTP Authorization 헤더 (Bearer scheme)
  - 포함 정보: user_id, email, role
  - 서명: HS256 (JWT_SECRET 사용)

- **Refresh Token**
  - 만료 시간: 7일 (설정 가능)
  - 저장소: Redis (key: `refresh_token:{user_id}`, value: token)
  - 전달 방식: httpOnly Cookie (HTTPS 필수)
  - 갱신: Access Token 만료 시 Refresh Token으로 새 Access Token 발급

**로그아웃 처리**
- Refresh Token을 Redis Blacklist에 등록 (key: `blacklist:{token}`, TTL: Refresh Token 남은 유효시간)
- 모든 기기에서 동시 로그아웃 가능 (해당 user_id의 모든 토큰 무효화)

**API Gateway JWT 검증**
- 모든 외부 요청에 대해 JWT 유효성 검증
- 토큰 없음 → 401 Unauthorized
- 토큰 만료됨 → 401 Unauthorized (클라이언트에서 Refresh Token으로 재발급)
- 토큰 서명 검증 실패 → 401 Unauthorized
- 토큰이 Blacklist에 있음 → 401 Unauthorized

**Role 기반 접근 제어 (RBAC)**
- CUSTOMER: 일반 고객
- ADMIN: 식당 관리자
- 권한별 접근 제한은 각 마이크로서비스에서 처리

#### 5.2 데이터베이스 설계

**users 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, BIGINT)
name (VARCHAR(100), NOT NULL)
email (VARCHAR(255), NOT NULL, UNIQUE, INDEX)
phone (VARCHAR(20), NOT NULL)
password (VARCHAR(255), NOT NULL) -- BCrypt 해싱
role (ENUM('CUSTOMER', 'ADMIN'), NOT NULL, DEFAULT='CUSTOMER')
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
updated_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
deleted_at (DATETIME, DEFAULT=NULL) -- 소프트 삭제 (선택사항)
```

**categories 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, INT)
name (VARCHAR(50), NOT NULL, UNIQUE) -- 한식, 중식, 일식 등
description (TEXT, DEFAULT=NULL)
image_url (VARCHAR(500), DEFAULT=NULL) -- 카테고리 이미지
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
```

**menus 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, INT)
name (VARCHAR(200), NOT NULL)
category_id (INT, NOT NULL, FK → categories.id)
description (TEXT, DEFAULT=NULL)
price (INT, NOT NULL) -- 원단위, 양수
image_url (VARCHAR(500), NOT NULL)
is_available (BOOLEAN, DEFAULT=TRUE) -- 노출 여부
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
updated_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
```

**orders 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, BIGINT)
user_id (BIGINT, NOT NULL, FK → users.id)
total_price (INT, NOT NULL) -- 최종 결제 금액 (쿠폰 적용 후)
coupon_id (INT, DEFAULT=NULL, FK → coupons.id)
discount_amount (INT, DEFAULT=0) -- 쿠폰/할인 금액
status (ENUM('PENDING', 'PREPARING', 'DONE'), NOT NULL, DEFAULT='PENDING')
  -- PENDING: 대기 중
  -- PREPARING: 준비 중
  -- DONE: 완료
special_request (TEXT, DEFAULT=NULL) -- 특별 요청사항
delivery_address (VARCHAR(500), DEFAULT=NULL) -- 배송지
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
updated_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
```

**order_items 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, BIGINT)
order_id (BIGINT, NOT NULL, FK → orders.id)
menu_id (INT, NOT NULL, FK → menus.id)
quantity (INT, NOT NULL, CHECK (quantity > 0))
price (INT, NOT NULL) -- 주문 시점의 단가 스냅샷 (이후 메뉴 가격 변동 영향 없음)
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)

INDEX (order_id)
```

**cart 테이블** (세션 기반, Redis 또는 DB 모두 가능)
```
id (PRIMARY KEY, AUTO_INCREMENT, BIGINT)
user_id (BIGINT, NOT NULL, FK → users.id, UNIQUE_KEY(user_id, menu_id))
menu_id (INT, NOT NULL, FK → menus.id)
quantity (INT, NOT NULL, CHECK (quantity > 0))
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
updated_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)

UNIQUE INDEX (user_id, menu_id) -- 사용자당 메뉴 1개만
```

**reviews 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, BIGINT)
menu_id (INT, NOT NULL, FK → menus.id)
user_id (BIGINT, NOT NULL, FK → users.id)
order_id (BIGINT, NOT NULL, FK → orders.id) -- 어느 주문에서 나온 리뷰인지 추적
rating (TINYINT, NOT NULL, CHECK (rating >= 1 AND rating <= 5))
comment (TEXT, NOT NULL, LENGTH >= 10)
image_urls (JSON, DEFAULT=NULL) -- 이미지 URL 배열 (선택사항)
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)
updated_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)

INDEX (menu_id)
INDEX (user_id)
INDEX (created_at)
UNIQUE INDEX (order_id) -- 주문당 리뷰 1개만
```

**coupons 테이블**
```
id (PRIMARY KEY, AUTO_INCREMENT, INT)
code (VARCHAR(50), NOT NULL, UNIQUE) -- SUMMER2024, SAVE10 등
discount_type (ENUM('FIXED_AMOUNT', 'PERCENTAGE'), NOT NULL)
  -- FIXED_AMOUNT: 고정금액 할인
  -- PERCENTAGE: 비율(%) 할인
discount_value (INT, NOT NULL) -- 할인금액 또는 할인율(%)
min_order_amount (INT, DEFAULT=0) -- 최소 주문 금액 (0이면 제한 없음)
max_discount_amount (INT, DEFAULT=NULL) -- 최대 할인금액 (비율 할인 시 상한선)
valid_from (DATETIME, NOT NULL)
valid_until (DATETIME, NOT NULL)
max_usage_per_user (INT, DEFAULT=1) -- 1인당 사용 횟수 제한
is_active (BOOLEAN, DEFAULT=TRUE)
created_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)

INDEX (code)
INDEX (valid_from, valid_until)
```

**coupon_usage 테이블** (선택사항: 쿠폰 사용 이력 추적)
```
id (PRIMARY KEY, AUTO_INCREMENT, BIGINT)
coupon_id (INT, NOT NULL, FK → coupons.id)
user_id (BIGINT, NOT NULL, FK → users.id)
order_id (BIGINT, NOT NULL, FK → orders.id)
used_at (DATETIME, NOT NULL, DEFAULT=CURRENT_TIMESTAMP)

INDEX (coupon_id, user_id)
```

#### 5.3 서비스별 권한

| 기능 | 고객 | 관리자 |
| --- | --- | --- |
| 메뉴 조회 | ✅ | ✅ |
| 메뉴 CRUD | ❌ | ✅ |
| 주문 생성 | ✅ | ❌ |
| 주문 조회 (본인) | ✅ | ❌ |
| 주문 조회 (전체) | ❌ | ✅ |
| 주문 상태 변경 | ❌ | ✅ |
| 리뷰 작성 | ✅ | ❌ |
| 리뷰 수정/삭제 (본인) | ✅ | ❌ |
| 리뷰 삭제 (모든) | ❌ | ✅ |
| 통계 조회 | ❌ | ✅ |
| 쿠폰 관리 | ❌ | ✅ |

---

### 6. AI 서비스 요구사항

#### 6.1 음식 추천 AI

**요구사항**
- 시간대 기반 추천: 점심/저녁/야식 등
- 최근 주문 집계 데이터 기반
- 카테고리 단위 추천 (특정 메뉴가 아닌 "한식", "양식" 등)
- 추천 이유 첨부

**구현 방식**
1. order-service에서 최근 7일 주문 집계 데이터 조회
   - 시간대별 주문 통계 (점심: 11~14시, 저녁: 17~21시 등)
   - 카테고리별 주문 건수
   - 예: "점심 시간에 한식과 중식 주문이 많습니다"

2. 집계 데이터를 OpenAI GPT API에 전송하여 추천 문구 생성
   - 입력: 시간대, 인기 카테고리, 과거 데이터
   - 출력: "점심엔 한식으로 든든하게, 중식으로 빠르게 한끼를 어떨까요?"

3. 추천 결과를 고객용 메인 화면에 표시
   - 회전 배너 형식 (1개씩 순차 표시)
   - 자동 갱신: 5분 마다
   - 이전/다음 버튼으로 수동 조회 가능

**API 호출 규칙**
```
POST /api/ai/recommendations
응답:
{
  "recommendation": "점심엔 한식 어떨까요?",
  "category": "한식",
  "timeSlot": "lunch",
  "confidence": 0.95,
  "refreshedAt": "2024-04-29T10:30:00Z"
}
```

#### 6.2 리뷰 요약 AI

**요구사항**
- 전체 리뷰 분석 → 3~5문장 자동 요약
- 긍정/부정 평가 추출
- 주요 언급 사항 (맛, 가성비, 신속성 등)
- 관리자용 후기 관리 페이지 상단에 표시

**구현 방식**
1. review-service에서 최근 리뷰 데이터 조회
   - 지난 30일 리뷰 조회
   - 별점 분포, 주요 키워드 추출
   - 예: 5점 리뷰 60%, 4점 20%, 별점: 4.6/5, 주요 키워드: "맛있음", "빨라요", "가성비"

2. 데이터를 OpenAI GPT API에 전송하여 요약 생성
   - 입력: 리뷰 목록, 별점, 키워드
   - 출력: "고객님들께서 본 메뉴를 매우 만족하시며, 특히 빠른 조리 시간과 훌륭한 맛으로 호평입니다. 다만 일부 고객님께서 양이 적다는 의견이 있습니다." (형식: 3~5문장)

3. 요약 결과를 관리자 화면에 표시
   - 갱신 빈도: 매일 자정 또는 수동 갱신 버튼
   - 생성 시간 표시

**API 호출 규칙**
```
POST /api/ai/review-summary
요청 본문:
{
  "menuId": 1,
  "reviewCount": 50,
  "averageRating": 4.6,
  "days": 30
}

응답:
{
  "summary": "고객님들께서 본 메뉴를 매우 만족하시며...",
  "sentiment": "positive",
  "highlights": ["빠른 조리", "훌륭한 맛", "친절한 서빙"],
  "concerns": ["양이 적음"],
  "generatedAt": "2024-04-29T00:00:00Z"
}
```

#### 6.3 자연어 검색 AI (프론트엔드 + 백엔드 연동)

**요구사항**
- 사용자 입력: "다이어트 음식", "매운 음식", "저렴한 음식" 등
- AI가 자연어를 해석하여 카테고리 + 가격대 필터링
- menu-service로 필터링된 메뉴 조회

**구현 방식**
1. 프론트엔드에서 검색어 입력받음
2. ai-service에 자연어 쿼리 전송
   ```
   POST /api/ai/search-intent
   요청: { "query": "다이어트 음식" }
   응답: {
     "categories": ["샐러드", "해산물"],
     "keywords": ["가벼움", "칼로리낮음"],
     "maxPrice": 20000,
     "confidence": 0.92
   }
   ```
3. 반환된 필터로 menu-service에서 메뉴 조회
4. 검색 결과 표시

#### 6.4 AI 서비스 보안

- OpenAI API 키는 .env 파일의 `OPENAI_API_KEY`로 관리
- API 키는 환경 변수로만 주입 (코드에 하드코딩 금지)
- ai-service는 Docker 내부 네트워크에서만 접근 가능
- 모든 AI API 호출은 로깅 (비용 추적)
- API 호출 제한: 시간당 최대 호출 횟수 제한 (rate limiting)

---

### 7. 배포 환경

#### 7.1 Docker 컨테이너 구성

**마이크로서비스별 Dockerfile**

각 서비스는 독립적인 Dockerfile을 가지고 있으며, 다단계 빌드(Multi-stage Build)를 권장합니다.

| 서비스 | Dockerfile 위치 | 기본 이미지 | 포트 |
| --- | --- | --- | --- |
| api-gateway | `api-gateway/Dockerfile` | `eclipse-temurin:17-jdk` | 8080 (내부), 8088 (외부) |
| auth-service | `auth-service/Dockerfile` | `eclipse-temurin:17-jdk` | 8081 |
| menu-service | `menu-service/Dockerfile` | `eclipse-temurin:17-jdk` | 8082 |
| order-service | `order-service/Dockerfile` | `eclipse-temurin:17-jdk` | 8083 |
| review-service | `review-service/Dockerfile` | `eclipse-temurin:17-jdk` | 8084 |
| ai-service | `ai-service/Dockerfile` | `python:3.10-slim` | 8085 |
| frontend-customer | `frontend-customer/Dockerfile` | `node:18-alpine` → `nginx:alpine` | 3000 |
| frontend-admin | `frontend-admin/Dockerfile` | `node:18-alpine` → `nginx:alpine` | 3001 |

**Dockerfile 설계 원칙**
- 다단계 빌드로 최종 이미지 크기 최소화
- 루트 사용자 실행 금지 (보안)
- 불필요한 레이어 제거
- .dockerignore 파일로 불필요한 파일 제외

#### 7.2 Docker Compose 구성

**docker-compose.yml 주요 사항**

1. **네트워크**
   - `msa-net`: 모든 백엔드 서비스가 연결되는 브리지 네트워크
   - 외부 포트: API Gateway (8088), 프론트엔드 (3000, 3001)만 노출
   - 내부 포트: 모든 서비스 간 통신은 Docker 내부 DNS 사용

2. **볼륨**
   - `auth-data`, `menu-data`, `order-data`, `review-data`: SQLite3 데이터 파일 저장
   - `redis-data`: Redis 영구 저장소
   - 이미지 저장소 (선택사항): 이미지 파일 저장 경로

3. **의존성**
   ```
   api-gateway → redis, 모든 마이크로서비스
   auth-service → redis
   ai-service → order-service, review-service
   frontend-customer → api-gateway
   frontend-admin → api-gateway
   ```

4. **환경 변수**
   - `.env` 파일로 중앙 관리
   - 민감한 정보: API 키, 데이터베이스 경로, 시크릿 키
   - 서비스별 환경 변수 설정

5. **재시작 정책**
   - `restart: unless-stopped`: 서비스 크래시 시 자동 재시작
   - 단, 명시적 종료 시 재시작하지 않음

#### 7.3 환경 변수 관리 (.env 파일)

**.env 파일 구조**
```env
# JWT 설정
JWT_SECRET=your_secret_key_here_minimum_32_characters
JWT_EXPIRATION_HOURS=0.5    # 30분
JWT_REFRESH_EXPIRATION_DAYS=7

# Redis 설정
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=          # 없으면 공백

# 데이터베이스 설정 (SQLite3는 파일 기반이므로 경로만 필요)
AUTH_DB_PATH=/data/auth.db
MENU_DB_PATH=/data/menu.db
ORDER_DB_PATH=/data/order.db
REVIEW_DB_PATH=/data/review.db
AI_DB_PATH=/data/ai.db

# AI 서비스 설정
OPENAI_API_KEY=sk-your_openai_key_here        # OpenAI API 키
OPENAI_MODEL=gpt-3.5-turbo                     # 또는 gpt-4
AI_RECOMMENDATION_REFRESH_MINUTES=5            # AI 추천 갱신 주기
AI_SUMMARY_REFRESH_HOURS=24                    # 리뷰 요약 갱신 주기

# 서비스 간 통신 (내부 네트워크)
AUTH_SERVICE_URL=http://auth-service:8080
MENU_SERVICE_URL=http://menu-service:8080
ORDER_SERVICE_URL=http://order-service:8080
REVIEW_SERVICE_URL=http://review-service:8080
AI_SERVICE_URL=http://ai-service:8085

# 보안 설정
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
SSL_ENABLED=false            # 개발: false, 프로덕션: true
DEBUG_MODE=true              # 개발: true, 프로덕션: false

# 로깅 설정
LOG_LEVEL=INFO               # DEBUG, INFO, WARN, ERROR
LOG_OUTPUT_PATH=/logs        # 로그 파일 저장 경로 (선택사항)
```

#### 7.4 로깅 및 모니터링

**로깅 전략**
- 모든 API 요청/응답 로깅 (API Gateway)
- 에러 및 예외 상황 로깅
- 로그 레벨: DEBUG, INFO, WARN, ERROR
- 로그 포맷: JSON 형식 (파싱 용이)
- 로그 저장소: 파일 (`/logs` 볼륨) 또는 ELK Stack (선택사항)

**모니터링 항목** (선택사항)
- CPU 사용률, 메모리 사용률
- 각 서비스의 응답 시간
- API 에러율
- Redis 메모리 사용률
- 데이터베이스 쿼리 성능

#### 7.5 외부 포트 노출 정책

**외부에 노출되는 포트**
```
8088   → API Gateway (모든 백엔드 요청의 진입점)
3000   → frontend-customer (고객용 웹)
3001   → frontend-admin (관리자용 웹)
```

**내부 포트 (외부 접근 불가)**
```
8080   → auth-service
8081   → auth-service
8082   → menu-service
8083   → order-service
8084   → review-service
8085   → ai-service
6379   → redis
```

#### 7.6 배포 프로세스

**개발 환경 (로컬)**
```powershell
# 1. 프로젝트 클론
git clone <repository-url>
cd restaurant-msa-project

# 2. .env 파일 생성 및 설정
cp .env.example .env
# .env 파일 수정

# 3. Docker Compose 실행
docker-compose up -d

# 4. 로그 확인
docker-compose logs -f

# 5. 종료
docker-compose down
```

**프로덕션 배포 (예정)**
- Docker Hub 또는 private registry에 이미지 푸시
- Kubernetes 또는 Docker Swarm 사용 (선택사항)
- 자동 배포: GitHub Actions / GitLab CI 연동
- 롤링 업데이트 (무중단 배포)
- 헬스 체크 및 자동 재시작

#### 7.7 CI/CD 파이프라인 (계획)

**GitHub Actions 워크플로우** (예시)
```yaml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      # Java 서비스 빌드
      - name: Build Java services
        run: ./gradlew build
      
      # Python 서비스 빌드
      - name: Build AI service
        run: |
          cd ai-service
          pip install -r requirements.txt
          # 테스트 실행 등
      
      # Docker 이미지 빌드 및 푸시
      - name: Build and push Docker images
        run: |
          docker-compose build
          # Docker Hub 또는 registry에 푸시
      
      # 배포 (예: Docker Swarm 또는 K8s)
      - name: Deploy
        run: |
          # 배포 스크립트 실행
```

---

### 8. 에러 처리 및 예외 사항

#### 8.1 HTTP 상태 코드

| 상태 코드 | 의미 | 사용 예시 |
| --- | --- | --- |
| 200 OK | 요청 성공 | 모든 정상 요청 |
| 201 Created | 리소스 생성 성공 | 주문 생성, 리뷰 작성 |
| 400 Bad Request | 잘못된 요청 | 필수 파라미터 누락 |
| 401 Unauthorized | 인증 실패 | JWT 토큰 없음/만료/유효하지 않음 |
| 403 Forbidden | 권한 부족 | 고객이 관리자 기능 접근 시도 |
| 404 Not Found | 리소스 없음 | 존재하지 않는 메뉴/주문 조회 |
| 409 Conflict | 충돌 | 이미 존재하는 이메일로 회원가입 |
| 429 Too Many Requests | 요청 초과 | API 호출 제한 초과 |
| 500 Internal Server Error | 서버 오류 | 예기치 않은 서버 에러 |
| 503 Service Unavailable | 서비스 불가 | 의존 서비스 다운 |

#### 8.2 에러 응답 형식

**통일된 에러 응답**
```json
{
  "error": {
    "code": "INVALID_JWT_TOKEN",
    "message": "JWT 토큰이 유효하지 않습니다.",
    "details": "Token expired at 2024-04-29 10:30:00",
    "timestamp": "2024-04-29T11:30:00Z"
  }
}
```

#### 8.3 예외 상황 처리

- **서비스 다운**: Circuit Breaker 패턴으로 실패한 요청 빠르게 반환
- **데이터베이스 연결 실패**: 재시도 로직 적용 (최대 3회)
- **API 타임아웃**: 기본 30초 설정
- **메모리 부족**: 컨테이너 스케일링 또는 메모리 할당 증가

---

### 9. 추가 고려사항

#### 9.1 성능 최적화

- **캐싱**: 자주 변하지 않는 메뉴/카테고리 데이터는 Redis에 캐싱
- **페이지네이션**: 대량 데이터 조회 시 필수 (기본: 10개 단위)
- **인덱싱**: 데이터베이스 쿼리 최적화 (email, menu_id, user_id 등)
- **쿼리 최적화**: N+1 문제 해결 (JPA Fetch Join 등)

#### 9.2 보안

- **암호화**: 비밀번호는 BCrypt로 해싱, 민감한 정보는 암호화
- **SQL 인젝션 방지**: Prepared Statement 사용
- **CORS**: 필요한 도메인만 화이트리스트 설정
- **HTTPS**: 프로덕션 환경에서는 필수
- **입력 검증**: 모든 사용자 입력에 대해 유효성 검사

#### 9.3 확장성

- **수평 확장**: API Gateway 뒤에 로드 밸런서 추가
- **데이터베이스 샤딩**: 대용량 데이터 처리 시 고려
- **메시지 큐**: 비동기 작업 처리 (예: 이메일 발송, AI 처리)
- **마이크로서비스 독립성**: 각 서비스는 독립적으로 배포/확장 가능

#### 9.4 테스트

- **단위 테스트**: 각 서비스별 비즈니스 로직
- **통합 테스트**: 서비스 간 통신 검증
- **E2E 테스트**: 전체 워크플로우 (주문 생성 ~ 완료)
- **성능 테스트**: 동시 사용자 부하 테스트

#### 9.5 문서화

- **API 문서**: Swagger/OpenAPI 사용 (자동 생성)
- **아키텍처 문서**: 시스템 다이어그램, 데이터 흐름
- **배포 가이드**: 환경 설정, 문제 해결 방법
- **개발자 가이드**: 코드 컨벤션, 깃 워크플로우

---

### 10. 주요 변경 사항 및 개선점 (요구사항 명세서 vs 실제 구조)

#### 🔍 발견된 이슈

| 항목 | 요구사항 명세서 | 실제 구조 | 개선 필요 |
| --- | --- | --- | --- |
| **AI 모델** | OpenAI GPT | ANTHROPIC API | ✅ docker-compose.yml에서 변경 필요 |
| **frontend-admin** | 포트 3001 | docker-compose.yml에 없음 | ✅ 추가 필요 |
| **포트 매핑** | 명시되지 않음 | 부분 구현 | ✅ 표준화 필요 |
| **테이블 구조** | 기본만 정의 | 세부 칼럼 없음 | ✅ 이 문서에서 상세 정의 |
| **에러 처리** | 언급 없음 | 구현 필요 | ✅ 통일된 형식 제시 |
| **환경 변수** | .env 관리 | 부분 구현 | ✅ 상세 .env 템플릿 제시 |
| **배포 프로세스** | 간략한 설명 | 구체적 절차 없음 | ✅ 상세 단계 제시 |
| **로깅** | 언급 없음 | 구현 필요 | ✅ 표준 로깅 전략 제시 |

#### ✅ 개선된 부분

1. **배포 환경 섹션 대폭 확장**
   - Docker Compose 설정 상세화
   - 환경 변수 관리 표준화
   - 배포 프로세스 구체화
   - CI/CD 파이프라인 기본안 제시

2. **데이터베이스 설계 상세화**
   - 각 테이블별 칼럼 타입 명시
   - 제약조건 및 인덱스 정의
   - 관계도 명시

3. **서비스별 포트 표준화**
   - 모든 서비스에 표준 포트 할당
   - 외부 포트 vs 내부 포트 구분

4. **에러 처리 및 예외 사항 추가**
   - HTTP 상태 코드 정의
   - 에러 응답 형식 통일
   - 예외 상황별 처리 방법

5. **추가 고려사항**
   - 성능 최적화 전략
   - 보안 체크리스트
   - 확장성 계획
   - 테스트 전략
   - 문서화 계획

---

### 부록 A: 빠른 시작 가이드

#### 1단계: 프로젝트 설정
```bash
git clone <repository>
cd restaurant-msa-project
cp .env.example .env
# .env 파일 수정
```

#### 2단계: Docker 실행
```bash
docker-compose up -d
```

#### 3단계: 서비스 확인
```
고객용 웹: http://localhost:3000
관리자 웹: http://localhost:3001
API Gateway: http://localhost:8088
```

#### 4단계: 테스트 계정
```
고객 계정: customer@example.com / password123
관리자 계정: admin@example.com / password123
```

---

### 부록 B: 문제 해결 가이드

**Q: 서비스가 계속 재시작되는 경우**
- A: 로그 확인: `docker-compose logs <service-name>`
- Redis 연결 확인, API 키 설정 확인

**Q: AI 추천이 나타나지 않는 경우**
- A: OPENAI_API_KEY 설정 확인
- AI 서비스 로그 확인: `docker-compose logs ai-service`

**Q: 데이터베이스 초기화 필요한 경우**
- A: 볼륨 삭제 후 재시작:
  ```bash
  docker-compose down -v
  docker-compose up -d
  ```
