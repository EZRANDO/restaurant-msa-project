#  MSA 기반 식당 주문 서비스

MSA 아키텍처 기반 온라인 주문 서비스. Docker + Spring Boot + Vue 3 + Python FastAPI + OpenAI

---

##  빠른 시작

```bash
# 1. 환경 설정
cp .env.example .env
# .env 파일에 OPENAI_API_KEY 입력

# 2. 서비스 실행
docker-compose up -d

# 3. 로그 확인
docker-compose logs -f
```

---

##  접속 방법

| 서비스 | URL | 포트 |
|--------|-----|------|
| 고객용 웹 | http://localhost:3000 | 3000 |
| 관리자 웹 | http://localhost:3001 | 3001 |
| **API Gateway** | http://localhost:8088 | 8088 |
| **Auth Swagger** | http://localhost:8081/swagger-ui.html | 8081 |
| **Menu Swagger** | http://localhost:8082/swagger-ui.html | 8082 |
| **Order Swagger** | http://localhost:8083/swagger-ui.html | 8083 |
| **Review Swagger** | http://localhost:8084/swagger-ui.html | 8084 |

---

##  기본 계정

```
고객:    customer@example.com / password123
관리자:  admin@example.com / password123
```

---

##  시스템 구조

```
┌──────────────────────────────────────────────┐
│  고객 웹 (3000)   │   관리자 웹 (3001)       │
└─────────┬─────────────────────┬──────────────┘
          │                     │
          └──────────┬──────────┘
                     │
            ┌────────▼─────────┐
            │  API Gateway     │ (8088)
            │  JWT 검증, 라우팅 │
            └────────┬─────────┘
                     │
   ┌─────────────────┼──────────────────┐
   │                 │                  │
┌──▼──┐  ┌──▼──┐ ┌──▼──┐  ┌──▼──┐  ┌──▼──┐
│Auth │  │Menu │ │Order│  │Review│ │AI   │
│8081 │  │8082 │ │8083 │  │8084 │  │8085 │
└──┬──┘  └──┬──┘ └──┬──┘  └──┬──┘  └─────┘
   │        │      │        │
   └────────┼──────┼────────┘
            │      │
         ┌──▼──┬───▼──┐
         │Redis│OpenAI│
         │6379 │API   │
         └─────┴──────┘
```

---

## 🔧 서비스 설명

| 서비스 | 설명 | 포트 |
|--------|------|------|
| **API Gateway** | 모든 API 요청의 진입점, JWT 검증 및 라우팅 | 8088 |
| **Auth Service** | 회원가입, 로그인, 토큰 관리 | 8081 |
| **Menu Service** | 메뉴 CRUD, 카테고리 관리 | 8082 |
| **Order Service** | 주문 생성/관리, 통계 제공 | 8083 |
| **Review Service** | 리뷰 CRUD, 평점 관리 | 8084 |
| **AI Service** | 메뉴 추천, 리뷰 요약, 자연어 검색 | 8085 |
| **Redis** | 캐싱, 토큰 저장, 세션 관리 | 6379 |

---

## OpenAI API 동작

1. **메뉴 추천**: 5분마다 주문 데이터를 분석하여 "점심엔 한식 어때요?" 형식의 추천 생성
2. **리뷰 요약**: 1시간마다 고객 리뷰를 분석하여 3~5문장의 종합 평가 자동 생성

---

##  API 문서

Swagger UI는 각 서비스 포트에서 확인합니다.

| 서비스 | Swagger UI |
|--------|------------|
| Auth Service | http://localhost:8081/swagger-ui.html |
| Menu Service | http://localhost:8082/swagger-ui.html |
| Order Service | http://localhost:8083/swagger-ui.html |
| Review Service | http://localhost:8084/swagger-ui.html |

**주요 엔드포인트:**
```
POST   /api/v1/auth/login              로그인
GET    /api/v1/menus                   메뉴 조회
POST   /api/v1/orders                  주문 생성
GET    /api/v1/reviews                 리뷰 조회
GET    /api/v1/ai/recommendations      메뉴 추천
```

---

##  환경 변수 (.env)

```env
# JWT
JWT_SECRET=your_secret_key_32_characters_minimum
JWT_EXPIRATION_HOURS=0.5

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# OpenAI
OPENAI_API_KEY=sk-your_actual_key_here
OPENAI_MODEL=gpt-3.5-turbo

# 로깅
LOG_LEVEL=INFO
DEBUG_MODE=true
```

---

##  유용한 명령어

```bash
# 모든 서비스 로그 확인
docker-compose logs -f

# 특정 서비스 재시작
docker-compose restart auth-service

# 컨테이너 상태 확인
docker-compose ps

# 데이터 초기화
docker-compose down -v
docker-compose up -d

# 컨테이너 내부 접속
docker-compose exec auth-service /bin/sh
```


---

## 상세 문서

더 자세한 요구사항은 아래 문서 참고:
- Auth Service Swagger: http://localhost:8081/swagger-ui.html
- Menu Service Swagger: http://localhost:8082/swagger-ui.html
- Order Service Swagger: http://localhost:8083/swagger-ui.html
- Review Service Swagger: http://localhost:8084/swagger-ui.html


