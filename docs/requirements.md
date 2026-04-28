## MSA 기반 식당 주문 서비스 — 요구사항 명세서

### 1. 프로젝트 개요

식당 고객용 웹과 관리자(식당)용 웹을 분리하여 제공하는 MSA 기반 온라인 주문 서비스입니다. AI 기능을 접목하여 메뉴 추천, 리뷰 자동 요약 등의 부가 서비스를 제공하며, 각 마이크로서비스는 Docker 컨테이너로 독립 배포됩니다.

---

### 2. 기술 스택

| 구분 | 기술 | 비고 |
| --- | --- | --- |
| 프론트엔드 | Vue 3 + Vite | 고객용 / 관리자용 별도 빌드 |
| 백엔드 | Java + Spring Boot | SQLite3 + Redis |
| AI 서비스 | Python + FastAPI | SQLite3 |
| AI 연동 | OpenAI GPT API | .env로 키 관리 |
| 인증 | JWT (Access + Refresh Token) | Redis Blacklist |
| 배포 | Docker + Docker Compose | 서비스별 독립 Dockerfile |
| 아키텍처 | MSA | API Gateway 통한 라우팅 |

---

### 3. 시스템 아키텍처

### 마이크로서비스 구성

- **api-gateway** : 모든 외부 요청 수신, JWT 유효성 검증, 인증 실패 시 요청 차단
- **auth-service** : 회원가입/로그인, Access·Refresh Token 발급·갱신, Redis Blacklist 처리
- **menu-service** : 메뉴 CRUD, 카테고리 관리
- **order-service** : 주문 생성·조회, 정산, 쿠폰 적용
- **review-service** : 리뷰 CRUD
- **ai-service** : 음식 추천, 리뷰 요약 (Python + FastAPI)
- **redis** : Refresh Token 저장, JWT Blacklist 관리

- 서비스 간 통신은 Docker 내부 네트워크를 사용하고, 외부 요청은 반드시 API Gateway를 통해서만 진입합니다.
- ai-service는 order-service의 REST API를 호출하여 최근 주문 데이터를 수집한다.
- 모든 서비스 간 통신은 HTTP 기반 REST API를 사용한다.
---

### 4. 기능 요구사항

### 4.1 고객용 웹

**메인 화면**

- 상단 고정: 키워드 검색바 (AI 기반 자연어 검색)
- 검색바 하단: 카테고리 메뉴바 (한식, 중식, 일식, 양식, 분식 등)
- 스크롤 하단: "이런 음식은 어때요?" AI 추천 음식 모음

**메뉴 검색**

- 자연어 키워드 검색 — 예: "다이어트 음식" → 샐러드, "매운 음식" → 떡볶이
- 카테고리 필터로 메뉴 조회

**장바구니**

- 메뉴 담기, 수량 조절, 삭제
- 합계 금액 실시간 표시
- 할인 쿠폰 코드 입력 필드

**주문**

- 장바구니 기반 주문 생성 (메뉴 항목, 수량, 금액 전송)
- 쿠폰 조건 충족 시 할인 자동 적용
- 주문 목록 조회 페이지 (상태: 대기 중 / 준비 중 / 완료)

**리뷰**

- 별점(1~5) + 코멘트 작성
- 본인 리뷰 수정·삭제
- 메뉴별 리뷰 목록 조회

**쿠폰**

**카테고리**
---

### 4.2 관리자용 웹

**메뉴 관리** : 메뉴 이름, 이미지, 상세 설명, 가격, 카테고리 기준으로 등록·수정·삭제·조회

**주문 관리** : 신규 주문 목록 조회, 주문 상태 변경 (대기 → 준비 중 → 완료)

**매출/통계** : 일별·월별·연도별 매출 집계, 날짜 범위 조회, 인기 메뉴(판매량 기준) 확인

**후기 관리** : 전체 리뷰 조회·삭제, AI 리뷰 요약으로 전반적인 고객 평가 자동 생성

**쿠폰 관리** : 쿠폰 코드·할인 금액(또는 비율)·최소 주문 금액·유효 기간 설정, 등록·수정·삭제

---

### 5. 백엔드 요구사항

### 인증·인가

- Access Token: 짧은 만료(예: 30분), 요청 헤더에 Bearer로 전달
- Refresh Token: 긴 만료(예: 7일), Redis에 저장
- 로그아웃 시 해당 토큰을 Redis Blacklist에 등록
- API Gateway에서 JWT 유효성 검증, 실패 시 401 반환 및 차단
- Role 기반 접근 제어: CUSTOMER / ADMIN 구분

### users 테이블

id, name, email(UNIQUE), phone, password(BCrypt 해싱), role(CUSTOMER/ADMIN), created_at

### orders / order_items 테이블

- orders: id, user_id, total_price, coupon_id, status(PENDING/PREPARING/DONE), created_at
- order_items: id, order_id, menu_id, quantity, price(주문 시점 단가 스냅샷)

### cart 테이블

cart_items

- id
- user_id (FK → users)
- menu_id (FK → menus)
- quantity
- created_at

### Review 테이블

id, comment, rating(1~5)

### 서비스별 권한

- 고객은 메뉴 **조회만** 가능, 주문은 **생성·조회만** 가능
- 리뷰는 본인 것만 수정·삭제
- 관리자만 메뉴 CRUD, 주문 상태 변경, 통계 조회 가능

---

### 6. AI 서비스 요구사항

**음식 추천 AI**

- 특정 가게 메뉴가 아닌 음식 카테고리 단위 추천
- order-service의 최근 주문 집계 데이터를 기반으로 추천
- 출력 형식: "점심엔 ○○ 메뉴 어때요?", "저녁엔 ○○ 메뉴 어때요?"
- 고객용 메인 화면 추천 섹션에 표시

**리뷰 요약 AI**

- 고객 리뷰 전체를 분석해 전반적인 평가를 3~5문장으로 자동 생성
- 관리자용 후기 관리 페이지 상단에 표시

공통: GPT API 키는 .env의 OPENAI_API_KEY로 관리, AI 서비스는 내부 네트워크에서만 접근 가능

---

### 7. 배포 환경

각 서비스(api-gateway, auth-service, menu-service, order-service, review-service, ai-service, frontend-customer, frontend-admin)별로 Dockerfile을 독립 작성하고, docker-compose.yml로 전체를 통합 실행합니다. 서비스 간 통신은 Docker 내부 네트워크를 사용하고, 외부에는 API Gateway와 프론트엔드 포트만 노출합니다. 환경 변수는 .env 파일로 주입하며 CI/CD 파이프라인 연동을 예정합니다.