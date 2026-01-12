# Issue Solve Multi-Agent System

멀티 에이전트 기반의 이슈 해결 챗봇 시스템으로, 사용자의 문제를 AI 에이전트가 협력하여 해결하는 플랫폼입니다.

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue.js Client │◄──►│ Spring Boot API │◄──►│  AI Agent (AI)  │
│   (Frontend)    │    │   (Backend)     │    │   (Python)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │     Kafka       │
                    │   (Message      │
                    │   Streaming)    │
                    └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │ Observability   │
                    │ (OTEL + LGTM)   │
                    │ Grafana/Tempo   │
                    └─────────────────┘
```

## 🚀 주요 기능

- **멀티 에이전트 대화**: 여러 AI 에이전트가 협력하여 복잡한 문제 해결
- **실시간 스트리밍**: Kafka를 통한 실시간 메시지 처리
- **채팅방 관리**: 다중 대화 세션 관리 및 기록 저장
- **사용자 관리**: 개인화된 대화 경험 제공
- **모니터링**: OpenTelemetry 기반 실시간 시스템 모니터링

## 📁 프로젝트 구조

```
issue-solve-multi-agent-system/
├── client/                     # Vue.js 프론트엔드
│   ├── src/                    # Vue 컴포넌트 및 로직
│   ├── dist/                   # 빌드 결과물
│   └── package.json           # Node.js 의존성
│
├── server/                     # Spring Boot 백엔드
│   ├── src/main/java/         # Java 소스 코드
│   │   └── net/riverfrot/multiagent/
│   │       ├── chatbot/       # 챗봇 도메인
│   │       ├── chatroom/      # 채팅방 도메인
│   │       ├── user/          # 사용자 도메인
│   │       ├── kafka/         # Kafka 통합
│   │       └── config/        # 설정 파일들
│   └── build.gradle           # Gradle 빌드 설정
│
├── ai-agent/                   # Python AI 에이전트
│   ├── src/                   # Python 소스 코드
│   │   ├── config/            # 설정 및 의존성
│   │   └── kafka_integration/ # Kafka 통합 로직
│   └── requirements.txt       # Python 의존성
│
├── otel/                       # OpenTelemetry 설정
│   ├── grafana-dashboards.yaml
│   ├── grafana-datasources.yaml
│   ├── multiagent-dashboard.json
│   └── otel-collector-config.yaml
│
├── docker-compose.intergation.yml  # 전체 시스템 통합 실행
├── docker-compose.kafka.yml        # Kafka 클러스터 설정
└── docker-compose.otel.yml         # Observability 스택
```

## 🛠️ 기술 스택

### 백엔드 (Spring Boot)
- **Java 21** - 최신 JDK 기능 활용
- **Spring Boot 3.3.5** - 엔터프라이즈 프레임워크
- **Spring Data JPA** - 데이터 액세스 레이어
- **Apache Kafka** - 메시지 스트리밍
- **H2 Database** - 개발용 인메모리 DB
- **OpenTelemetry** - 관찰가능성 (Observability)

### AI 에이전트 (Python)
- **FastAPI 0.121+** - 현대적 Python 웹 프레임워크
- **OpenAI API** - AI 모델 통합
- **Confluent Kafka** - Kafka Python 클라이언트
- **Pydantic 2.12+** - 데이터 검증 및 직렬화
- **OpenTelemetry** - 분산 추적

### 프론트엔드 (Vue.js)
- **Vue.js 2.5** - 프로그레시브 프레임워크
- **Bootstrap Vue** - UI 컴포넌트 라이브러리
- **Axios** - HTTP 클라이언트
- **Socket.io** - 실시간 통신
- **Vuex** - 상태 관리

### 인프라스트럭처
- **Apache Kafka 7.4.0** - 이벤트 스트리밍 플랫폼
- **Grafana OTEL-LGTM** - 통합 관찰가능성 스택
  - **Grafana** - 대시보드 및 시각화
  - **Tempo** - 분산 추적 백엔드
  - **Mimir** - 메트릭 스토리지
  - **Loki** - 로그 집계
- **Jaeger** - 분산 추적 (선택사항)

## 🚀 빠른 시작

### 전제 조건
- Docker & Docker Compose
- Java 21+
- Node.js 16+
- Python 3.9+

### 1. 전체 시스템 실행
```bash
# 모든 컴포넌트 통합 실행
docker-compose -f docker-compose.intergation.yml up -d

# 서비스 상태 확인
docker-compose -f docker-compose.intergation.yml ps
```

### 2. 개별 컴포넌트 실행

#### Spring Boot 서버
```bash
cd server
./gradlew bootRun
```

#### AI 에이전트
```bash
cd ai-agent
pip install -r requirements.txt
uvicorn src.main:app --host 0.0.0.0 --port 8001
```

#### Vue.js 클라이언트
```bash
cd client
npm install
npm run dev
```

### 3. 접속 정보
- **클라이언트 UI**: http://localhost:8080
- **Spring Boot API**: http://localhost:8080/api
- **AI 에이전트 API**: http://localhost:8001
- **Kafka UI**: http://localhost:8089
- **Grafana 대시보드**: http://localhost:3000 (admin/admin)
- **Jaeger UI**: http://localhost:16686 (선택사항)

## 📊 모니터링 및 관찰가능성

### OpenTelemetry 통합
- **메트릭 수집**: 애플리케이션 성능 지표
- **분산 추적**: 요청 흐름 추적
- **로그 집계**: 중앙화된 로그 관리
- **사전 구성된 대시보드**: Grafana 대시보드 자동 프로비저닝

### 주요 엔드포인트
- **헬스체크**: `GET /api/health`
- **메트릭**: `GET /actuator/prometheus`
- **Kafka 테스트**: `GET /api/kafka/test`

## 🔧 개발 가이드

### 프로젝트 구조 원칙
- **DDD (Domain-Driven Design)** 적용
- **계층형 아키텍처**: Presentation → Application → Domain → Infrastructure
- **의존성 역전 원칙**: 인터페이스 기반 설계
- **테스트 우선 개발**: JUnit5 + Mockito 활용

### 코딩 컨벤션
- **Java**: Google Java Style Guide 기반
- **Python**: PEP 8 준수
- **Vue.js**: Vue Style Guide 따름

### Git 워크플로
```bash
# 피처 브랜치 생성
git checkout -b feature/새로운기능

# 개발 완료 후 커밋
git commit -m "feat(domain): 새로운 기능 구현

```

## 테스트

### 전체 테스트 실행
```bash
# Spring Boot 테스트
cd server && ./gradlew test

# Python 테스트  
cd ai-agent && python -m pytest

# 프론트엔드 테스트
cd client && npm test
```

### 통합 테스트
```bash
# Kafka 통합 테스트
curl http://localhost:8080/api/kafka/integration/test

# AI 에이전트 연동 테스트
curl http://localhost:8080/api/chat -d '{"message":"테스트"}'
```

## 🚀 배포

### Docker 컴포즈 배포
```bash
# 프로덕션 환경 배포
docker-compose -f docker-compose.intergation.yml up -d

# 로그 확인
docker-compose -f docker-compose.intergation.yml logs -f
```
