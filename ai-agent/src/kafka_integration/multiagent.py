"""
Multiagent 시스템 - 다양한 AI 에이전트 타입 지원
사용자가 요청에 따라 다른 전문화된 에이전트를 선택할 수 있음
"""
import asyncio
import logging
import random
from typing import Dict, List
from enum import Enum

logger = logging.getLogger(__name__)


class AgentType(Enum):
    """AI 에이전트 타입 정의"""
    GENERAL = "GENERAL"          # 일반적인 대화 에이전트
    CODE = "CODE"                # 코드 분석 및 프로그래밍 지원
    RAG = "RAG"                  # 검색 증강 생성 (문서 기반)
    SEARCH = "SEARCH"            # 웹 검색 및 정보 수집
    SUPERVISOR = "SUPERVISOR"    # 다중 에이전트 조정


class MultiAgent:
    """멀티 에이전트 시스템 구현"""
    
    def __init__(self):
        self.agents = {
            AgentType.GENERAL: GeneralAgent(),
            AgentType.CODE: CodeAgent(),
            AgentType.RAG: RAGAgent(),
            AgentType.SEARCH: SearchAgent(),
            AgentType.SUPERVISOR: SupervisorAgent()
        }
        logger.info("MultiAgent system initialized with 5 specialized agents")
    
    async def process_request(self, message: str, agent_type: str = "GENERAL") -> tuple[str, str]:
        """
        요청을 처리하고 응답 생성
        
        Args:
            message: 사용자 메시지
            agent_type: 에이전트 타입 (선택사항)
            
        Returns:
            tuple[응답_메시지, 사용된_에이전트_타입]
        """
        try:
            # 에이전트 타입 자동 감지 또는 지정된 타입 사용
            selected_agent_type = self._detect_agent_type(message, agent_type)
            
            logger.info(f"Processing with agent: {selected_agent_type.value}")
            
            # 선택된 에이전트로 요청 처리
            agent = self.agents[selected_agent_type]
            response = await agent.process(message)
            
            return response, selected_agent_type.value
            
        except Exception as e:
            logger.error(f"Error in multiagent processing: {e}")
            return f"죄송합니다. 처리 중 오류가 발생했습니다: {str(e)}", AgentType.GENERAL.value
    
    def _detect_agent_type(self, message: str, requested_type: str) -> AgentType:
        """메시지 내용과 요청 타입을 기반으로 적절한 에이전트 선택"""
        
        # 명시적으로 에이전트 타입이 요청된 경우
        if requested_type and requested_type != "GENERAL":
            try:
                return AgentType(requested_type)
            except ValueError:
                logger.warning(f"Invalid agent type requested: {requested_type}, falling back to auto-detection")
        
        # 메시지 내용 기반 자동 감지
        message_lower = message.lower()
        
        # 코드 관련 키워드
        code_keywords = ['코드', 'code', 'python', 'java', 'javascript', 'bug', '버그', '함수', 'function', 'class']
        if any(keyword in message_lower for keyword in code_keywords):
            return AgentType.CODE
        
        # 검색 관련 키워드
        search_keywords = ['검색', 'search', '찾아', 'find', '정보', 'information', '뉴스', 'news']
        if any(keyword in message_lower for keyword in search_keywords):
            return AgentType.SEARCH
        
        # RAG 관련 키워드 (문서, 자료 관련)
        rag_keywords = ['문서', 'document', '자료', 'material', '논문', 'paper', '참고', 'reference']
        if any(keyword in message_lower for keyword in rag_keywords):
            return AgentType.RAG
        
        # 복잡한 요청 (Supervisor 필요)
        if len(message.split()) > 20 or '단계' in message_lower or 'step' in message_lower:
            return AgentType.SUPERVISOR
        
        # 기본값: GENERAL
        return AgentType.GENERAL


class BaseAgent:
    """기본 에이전트 클래스"""
    
    def __init__(self, name: str, speciality: str):
        self.name = name
        self.speciality = speciality
    
    async def process(self, message: str) -> str:
        """에이전트별 메시지 처리 (하위 클래스에서 구현)"""
        raise NotImplementedError


class GeneralAgent(BaseAgent):
    """일반 대화 에이전트"""
    
    def __init__(self):
        super().__init__("General AI", "일반적인 대화 및 질문 응답")
    
    async def process(self, message: str) -> str:
        await asyncio.sleep(0.3)  # 처리 시간 시뮬레이션
        
        if "안녕" in message or "hello" in message.lower():
            return f"안녕하세요! 🤖 General AI Agent입니다. 무엇을 도와드릴까요?"
        elif "고마워" in message or "thank" in message.lower():
            return "천만에요! 더 도움이 필요하시면 언제든지 말씀해 주세요. 😊"
        elif "날씨" in message:
            return "🌤️ 죄송합니다. 실시간 날씨 정보는 제공하지 않습니다. 기상청 앱을 확인해 보세요!"
        else:
            return f"💬 General AI: '{message}' 메시지를 잘 받았습니다. 일반적인 질문이나 대화를 도와드릴 수 있어요!"


class CodeAgent(BaseAgent):
    """코드 분석 및 프로그래밍 지원 에이전트"""
    
    def __init__(self):
        super().__init__("Code AI", "프로그래밍 및 코드 분석")
    
    async def process(self, message: str) -> str:
        await asyncio.sleep(0.5)  # 코드 분석 시간
        
        if "python" in message.lower():
            return """🐍 **Python 전문가 AI**
            
Python 관련 질문을 해주세요:
- 코드 리뷰 및 최적화
- 버그 디버깅 도움
- 알고리즘 구현 가이드
- Django/Flask 웹 개발
- 데이터 분석 (pandas, numpy)

예시: "리스트 컴프리헨션으로 피보나치 수열 만들기"
            """
        elif "java" in message.lower():
            return """☕ **Java 전문가 AI**
            
Java/Spring 관련 도움을 드립니다:
- Spring Boot 애플리케이션 개발
- JPA/Hibernate ORM 최적화
- 마이크로서비스 아키텍처
- 성능 튜닝 및 메모리 관리
- JUnit 테스트 작성

예시: "Spring Boot에서 Kafka 연동 방법"
            """
        elif any(word in message.lower() for word in ['bug', '버그', 'error', '에러']):
            return """🔍 **디버깅 전문가 AI**
            
버그 해결을 도와드립니다:
1. 에러 메시지와 스택 트레이스 공유
2. 문제가 발생한 코드 부분 제시
3. 예상되는 동작과 실제 동작 설명
4. 사용 중인 환경 (언어, 프레임워크, 버전)

🛠️ 체계적인 디버깅 접근법을 제공하겠습니다!
            """
        else:
            return f"👨‍💻 **Code AI**: '{message}' - 프로그래밍 관련 질문이군요! 구체적인 언어나 기술을 명시해 주시면 더 정확한 도움을 드릴 수 있습니다."


class RAGAgent(BaseAgent):
    """검색 증강 생성 에이전트 (문서 기반)"""
    
    def __init__(self):
        super().__init__("RAG AI", "문서 검색 및 지식 기반 응답")
        # 시뮬레이션을 위한 가상 문서 데이터베이스
        self.document_db = {
            "kafka": "Apache Kafka는 분산 스트리밍 플랫폼으로, 실시간 데이터 파이프라인과 스트리밍 애플리케이션을 구축할 수 있게 해줍니다.",
            "spring": "Spring Framework는 Java 기업용 애플리케이션 개발을 위한 포괄적인 프로그래밍 및 구성 모델을 제공합니다.",
            "python": "Python은 배우기 쉬운 문법과 강력한 라이브러리 생태계를 갖춘 고급 프로그래밍 언어입니다."
        }
    
    async def process(self, message: str) -> str:
        await asyncio.sleep(0.7)  # 문서 검색 시간
        
        # 간단한 키워드 매칭으로 관련 문서 찾기
        relevant_docs = []
        for keyword, content in self.document_db.items():
            if keyword in message.lower():
                relevant_docs.append(f"📄 **{keyword.upper()}**: {content}")
        
        if relevant_docs:
            response = "📚 **RAG AI - 문서 검색 결과**\n\n"
            response += "\n\n".join(relevant_docs)
            response += "\n\n💡 더 구체적인 질문이 있으시면 말씀해 주세요!"
            return response
        else:
            return f"""📚 **RAG AI**: '{message}'에 대한 문서를 검색했습니다.

🔍 **검색 결과**: 직접 매칭되는 문서를 찾지 못했습니다.

💡 **제안**: 다음과 같은 키워드로 다시 검색해 보세요:
- kafka, spring, python
- 또는 더 구체적인 기술 용어 사용

📖 현재 이용 가능한 문서 영역:
- 시스템 아키텍처
- API 문서
- 기술 가이드라인
            """


class SearchAgent(BaseAgent):
    """웹 검색 및 정보 수집 에이전트"""
    
    def __init__(self):
        super().__init__("Search AI", "웹 검색 및 실시간 정보 수집")
    
    async def process(self, message: str) -> str:
        await asyncio.sleep(0.6)  # 검색 시간
        
        # 시뮬레이션된 검색 결과
        search_topics = {
            "뉴스": ["기술 동향", "AI 발전", "프로그래밍 트렌드"],
            "날씨": ["서울 맑음 23°C", "미세먼지 좋음", "내일 비 예보"],
            "주식": ["코스피 상승", "기술주 강세", "반도체 업종 주목"],
        }
        
        # 키워드 기반 가상 검색 결과
        for topic, results in search_topics.items():
            if topic in message:
                response = f"🔍 **Search AI - '{topic}' 검색 결과**\n\n"
                for i, result in enumerate(results, 1):
                    response += f"{i}. {result}\n"
                response += f"\n⏰ 검색 시간: {asyncio.get_event_loop().time():.0f}"
                response += "\n📌 실제 서비스에서는 실시간 웹 검색 결과를 제공합니다."
                return response
        
        return f"""🔍 **Search AI**: '{message}' 검색을 수행했습니다.

🌐 **웹 검색 기능**:
- 실시간 뉴스 및 정보
- 기술 문서 및 가이드
- 최신 트렌드 및 동향
- 공식 문서 및 API 레퍼런스

💡 **검색 팁**: '뉴스', '날씨', '주식' 등의 키워드로 테스트해 보세요!

⚠️  현재는 시뮬레이션 모드입니다. 실제 구현에서는 외부 API를 연동합니다.
        """


class SupervisorAgent(BaseAgent):
    """다중 에이전트 조정 및 복잡한 작업 처리"""
    
    def __init__(self):
        super().__init__("Supervisor AI", "다중 에이전트 조정 및 작업 분할")
    
    async def process(self, message: str) -> str:
        await asyncio.sleep(1.0)  # 복잡한 분석 시간
        
        # 작업 분할 시뮬레이션
        tasks = self._analyze_complex_request(message)
        
        response = f"""🎯 **Supervisor AI - 복합 작업 분석**

📋 **요청 분석**: '{message}'

🔄 **작업 분할 계획**:
"""
        
        for i, task in enumerate(tasks, 1):
            response += f"{i}. {task['action']} → {task['agent']} 에이전트\n"
        
        response += f"""
⚡ **실행 순서**: 순차 실행 (약 {len(tasks) * 2}초 소요 예상)
🤝 **협업 방식**: 각 에이전트 결과를 통합하여 최종 응답 생성

💡 실제 구현에서는 각 세부 에이전트를 순차적으로 호출하고 결과를 통합합니다.
        """
        
        return response
    
    def _analyze_complex_request(self, message: str) -> List[Dict]:
        """복잡한 요청을 세부 작업으로 분할"""
        tasks = []
        
        if any(word in message.lower() for word in ['코드', 'code', 'programming']):
            tasks.append({"action": "코드 분석 및 최적화", "agent": "CODE"})
        
        if any(word in message.lower() for word in ['검색', 'search', '찾기']):
            tasks.append({"action": "관련 정보 검색", "agent": "SEARCH"})
        
        if any(word in message.lower() for word in ['문서', 'document', '자료']):
            tasks.append({"action": "문서 검색 및 분석", "agent": "RAG"})
        
        if not tasks:  # 작업을 감지하지 못한 경우
            tasks = [
                {"action": "일반적인 질문 처리", "agent": "GENERAL"},
                {"action": "추가 정보 수집", "agent": "SEARCH"}
            ]
        
        tasks.append({"action": "결과 통합 및 최종 응답 생성", "agent": "SUPERVISOR"})
        
        return tasks


# 전역 인스턴스 (싱글톤 패턴)
multi_agent_instance = None

def get_multiagent_system() -> MultiAgent:
    """멀티에이전트 시스템 인스턴스 반환 (싱글톤)"""
    global multi_agent_instance
    if multi_agent_instance is None:
        multi_agent_instance = MultiAgent()
        logger.info("MultiAgent system instance created")
    return multi_agent_instance