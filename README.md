# issue-solve-multi-agent-system
OSS 이슈 해결을 위한 멀티 에이전트 시스템
해당 멀티 에이전트 repository는 아래의 시스템 구조도를 통해 이슈를 수집 및 해결 진행 예정입니다.
추후 https://github.com/riverfrot/advanced-rag-system repository를 MCP HTTP Streamable 서버로 구동 후 
tools로 사용 예정

## Architecture

### 전체 시스템 구조도
```mermaid
graph TB
    Start([GitHub Issue]) --> Ingest[Issue Ingestion]
    Ingest --> Planner[🎯 Planner Agent<br/>이슈 분석 및 전략 수립]
    
    Planner --> Researcher[🔍 Researcher Agent<br/>코드베이스 분석]
    Researcher --> Resolver[⚡ Resolver Agent<br/>해결책 구현]
    Resolver --> Critic[✅ Critic Agent<br/>품질 검증]
    
    Critic -->|검증 통과| Reporter[📝 Reporter Agent<br/>보고서 작성]
    Critic -->|계획 문제| Planner
    Critic -->|분석 부족| Researcher
    Critic -->|구현 문제| Resolver
    
    Reporter --> PR([GitHub Pull Request])
    
    style Start fill:#2196F3,stroke:#0D47A1,stroke-width:4px,color:#fff
    style PR fill:#2196F3,stroke:#0D47A1,stroke-width:4px,color:#fff
    style Planner fill:#FF9800,stroke:#E65100,stroke-width:4px,color:#fff
    style Researcher fill:#9C27B0,stroke:#4A148C,stroke-width:4px,color:#fff
    style Resolver fill:#4CAF50,stroke:#1B5E20,stroke-width:4px,color:#fff
    style Critic fill:#F44336,stroke:#B71C1C,stroke-width:4px,color:#fff
    style Reporter fill:#E91E63,stroke:#880E4F,stroke-width:4px,color:#fff
```


### RAG 시스템 통합 (모든 에이전트 공통 도구)
```mermaid
graph LR
    subgraph Agents[" 🤖 모든 에이전트 "]
        A1[Planner]
        A2[Researcher]
        A3[Resolver]
        A4[Critic]
        A5[Reporter]
    end
    
    subgraph RAG[" 🔧 RAG System (MCP Server) "]
        direction TB
        Repo[Repository<br/>Codebase] --> Chunk[Chunking]
        Chunk --> Embed[Embedding]
        Embed --> Vector[(Vector Store<br/>ChromaDB)]
        Vector --> Search[Hybrid Search<br/>Dense + Sparse]
    end
    
    subgraph External[" 🌐 External Tools "]
        Tavily[Tavily API<br/>Web Search]
        GitHub[GitHub API<br/>Issues/PRs]
    end
    
    A1 -.->|MCP Protocol| Search
    A2 -.->|MCP Protocol| Search
    A3 -.->|MCP Protocol| Search
    A4 -.->|MCP Protocol| Search
    A5 -.->|MCP Protocol| Search
    
    A2 -.-> Tavily
    A2 -.-> GitHub
    A5 -.-> GitHub
    
    style RAG fill:#e8f4f8
    style External fill:#f8e8e8
```

### 피드백 루프 상세
```mermaid
stateDiagram-v2
    [*] --> Planner: 이슈 접수
    
    Planner --> Researcher: 전략 수립 완료
    Researcher --> Resolver: 분석 완료
    Resolver --> Critic: 구현 완료
    
    state Critic {
        [*] --> Validate
        Validate --> CheckPlan: 계획 검증
        Validate --> CheckResearch: 분석 검증
        Validate --> CheckCode: 코드 검증
        
        CheckPlan --> [*]: OK
        CheckResearch --> [*]: OK
        CheckCode --> [*]: OK
    }
    
    Critic --> Reporter: 모든 검증 통과
    Critic --> Planner: 계획 수정 필요
    Critic --> Researcher: 추가 분석 필요
    Critic --> Resolver: 코드 수정 필요
    
    Reporter --> [*]: PR 생성 완료
    
    note right of Critic
        최대 5회 반복
        (무한 루프 방지)
    end note
```


### 에이전트 역할

| 에이전트 | 역할 | 주요 기능 |
|---------|------|----------|
| **Planner** | 이슈 분석 및 해결 계획 수립 | 이슈 분류, 우선순위 설정, 해결 전략 수립 |
| **Researcher** | 코드베이스 분석 및 정보 수집 | RAG 시스템 활용, 관련 코드 탐색, 외부 리소스 조사 |
| **Critic** | Resolver가 해결한 이슈에 대해 한번 더평가 | 코드 리뷰, 테스트 계획, 잠재적 이슈 식별 |
| **Resolver** | 실제 해결책 구현 | 코드 수정, 패치 생성, 테스트 실행 |
| **Reporter** | 결과 보고 및 문서화 | 해결 보고서 작성, 변경사항 문서화 |


## Installation

### Prerequisites

- Python 3.8+
- Docker & Docker Compose
- Kubernetes cluster (선택사항)


### **TODO:**
- [ ] Planner 에이전트 구현
- [ ] Researcher 에이전트 구현
- [ ] FastMCP 서버 연동
- [ ] 기본 API 엔드포인트 구현
- [ ] 비동기 큐 시스템 구현
- [ ] Kubernetes 기반 자동 스케일링
- [ ] 웹 대시보드 개발
- [ ] 고급 모니터링 및 로깅
- [ ] 다중 Repository 동시 처리
- [ ] AI 모델 성능 최적화

## 라이선스

MIT License
