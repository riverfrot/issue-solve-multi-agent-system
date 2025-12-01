# Multi-Agent Chatbot Client

## 프로젝트 구조

```
client/
├── build/                 # webpack 빌드 설정
├── config/                # 환경 설정
├── src/
│   ├── assets/           # SCSS 스타일 파일
│   ├── components/       # Vue 컴포넌트
│   ├── plugins/         # Vue 플러그인
│   ├── router/          # Vue Router 설정
│   ├── services/        # API 서비스
│   ├── store.js         # Vuex 상태 관리
│   ├── views/           # 페이지 컴포넌트
│   ├── App.vue          # 루트 컴포넌트
│   └── main.js          # 애플리케이션 진입점
├── static/              # 정적 파일
└── index.html           # HTML 템플릿
```

## 기술 스택

- **Vue.js 2.5**: 프론트엔드 프레임워크
- **Bootstrap Vue**: UI 컴포넌트
- **Vuex**: 상태 관리
- **Vue Router**: 라우팅
- **Axios**: HTTP 클라이언트
- **SCSS**: 스타일링
- **ESLint + Prettier**: 코드 품질 관리

## 주요 특징

### 코딩 스타일 변경사항
- **Composition API → Options API**: Vue 2 스타일 코딩
- **TailwindCSS → Bootstrap + SCSS**: 클래식한 스타일링 방식
- **인라인 스타일 → 별도 스타일 파일**: 관심사 분리
- **모던 문법 → 전통적인 구조**: 가독성 및 유지보수성 향상

### 컴포넌트 구조
- `ChatHeader.vue`: 헤더 및 연결 상태 표시
- `ChatFooter.vue`: 푸터 정보
- `ChatInput.vue`: 메시지 입력 컴포넌트
- `ChatRoom.vue`: 메인 채팅 인터페이스
- `Loader.vue`: 로딩 인디케이터

### API 서비스
- 체계적인 에러 처리
- 인터셉터를 통한 요청/응답 관리
- 토스트 알림 통합

## 설치 및 실행

### 의존성 설치
\`\`\`bash
npm install
\`\`\`

### 개발 서버 실행
\`\`\`bash
npm run dev
\`\`\`

### 빌드
\`\`\`bash
npm run build
\`\`\`

### 린트 검사
\`\`\`bash
npm run lint
\`\`\`

## 환경 변수

\`config/dev.env.js\` 파일에서 개발 환경 설정:
- `API_URL`: 백엔드 API 서버 주소 (기본값: http://localhost:8000)

## 개발자 도구

- **ESLint**: Airbnb 스타일 가이드 기반
- **Prettier**: 코드 포매팅
- **EditorConfig**: 에디터 설정 통일
- **Babel**: ES6+ 트랜스파일링

## API 인터페이스

### 주요 API 메서드
- `sendMessage(message, sessionId)`: 메시지 전송
- `getChatHistory(sessionId)`: 채팅 기록 조회
- `healthCheck()`: 서버 상태 확인
- `clearSession(sessionId)`: 세션 초기화

### 에이전트 시스템
- **Supervisor**: 요청 분석 및 라우팅
- **RAG Agent**: 문서 검색
- **Code Agent**: 코드 실행
- **Search Agent**: 웹 검색
- **General Agent**: 일반 대화

## 라이선스
