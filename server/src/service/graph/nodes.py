from typing import Dict, Any
from domain.models.graph_state import GraphState, AgentDecision
from domain.models.agent import AgentType, AgentRequest, AgentResponse
from service.agent.RAG_agent import RAGAgent
from service.agent.search_agent import SearchAgent
from service.agent.general_agent import GeneralAgent
from infrastructure.llm.openai_client import OpenAIClient
from utils.logger import logger
import time


class LangGraphNodes:
    """LangGraph 노드 컬렉션"""
    
    def __init__(
        self,
        openai_client: OpenAIClient
    ):
        # 에이전트 인스턴스 생성
        self.rag_agent = RAGAgent(openai_client)
        self.search_agent = SearchAgent(openai_client)
        self.general_agent = GeneralAgent(openai_client)
        
        # 서비스 매핑
        self.services = {
            AgentType.RAG: self.rag_agent,
            AgentType.SEARCH: self.search_agent,
            AgentType.GENERAL: self.general_agent
        }
        
        logger.info("LangGraph nodes initialized")

    async def supervisor_node(self, state: GraphState) -> Dict[str, Any]:
        """
        Supervisor 노드 - 사용자 의도 분석 및 라우팅 결정
        """
        logger.info(f"🎯 Supervisor analyzing: {state.query}")
        
        start_time = time.time()
        
        # 의도 분석
        agent_type = self._analyze_intent(state.query)
        
        # 상태 업데이트
        state.current_agent = agent_type
        state.agent_route.append(agent_type)
        state.reasoning.append(f"🎯 Supervisor: Routing to {agent_type.value} agent")
        
        # 메타데이터 업데이트
        processing_time = (time.time() - start_time) * 1000
        state.metadata.update({
            "supervisor_decision": agent_type.value,
            "supervisor_latency_ms": processing_time,
            "routing_confidence": 0.85  # TODO: 실제 신뢰도 계산
        })
        
        logger.info(f"🎯 Supervisor decision: {agent_type.value}")
        
        return {
            "current_agent": agent_type,
            "agent_route": state.agent_route,
            "reasoning": state.reasoning,
            "metadata": state.metadata
        }

    async def rag_agent_node(self, state: GraphState) -> Dict[str, Any]:
        """RAG 에이전트 노드"""
        return await self._execute_agent_node(state, AgentType.RAG, "📚")

    async def code_agent_node(self, state: GraphState) -> Dict[str, Any]:
        """Code 에이전트 노드"""
        # Code 에이전트는 아직 구현되지 않았으므로 General 에이전트로 폴백
        logger.warning("Code agent not implemented, falling back to General agent")
        return await self._execute_agent_node(state, AgentType.GENERAL, "💻")

    async def search_agent_node(self, state: GraphState) -> Dict[str, Any]:
        """Search 에이전트 노드"""
        return await self._execute_agent_node(state, AgentType.SEARCH, "🔍")

    async def general_agent_node(self, state: GraphState) -> Dict[str, Any]:
        """General 에이전트 노드"""
        return await self._execute_agent_node(state, AgentType.GENERAL, "💬")

    async def aggregator_node(self, state: GraphState) -> Dict[str, Any]:
        """
        Aggregator 노드 - 다중 에이전트 결과 통합
        """
        logger.info("🔄 Aggregating multi-agent responses")
        
        # 중간 응답들이 있으면 통합
        if state.intermediate_responses:
            responses = []
            for agent_type, response in state.intermediate_responses.items():
                responses.append(f"{agent_type}: {response}")
            
            # 응답 통합
            state.response = "\n\n".join(responses)
            state.reasoning.append("🔄 Aggregated multiple agent responses")
        
        # 완료 표시
        state.is_complete = True
        
        return {
            "response": state.response,
            "is_complete": state.is_complete,
            "reasoning": state.reasoning
        }

    async def _execute_agent_node(
        self, 
        state: GraphState, 
        agent_type: AgentType, 
        emoji: str
    ) -> Dict[str, Any]:
        """공통 에이전트 노드 실행 로직"""
        logger.info(f"{emoji} Executing {agent_type.value} agent")
        
        start_time = time.time()
        
        try:
            # 에이전트 서비스 가져오기
            service = self.services[agent_type]
            
            # 요청 생성
            request = AgentRequest(
                query=state.query,
                context={
                    "session_id": state.session_id,
                    "history": state.history,
                    "metadata": state.metadata
                },
                session_id=state.session_id
            )
            
            # 에이전트 실행 (동기 방식)
            response = service.process(request)
            
            # 응답 처리
            state.response = response.content
            state.intermediate_responses[agent_type.value] = response.content
            state.reasoning.append(f"{emoji} {agent_type.value} agent completed")
            
            # 메타데이터 업데이트
            processing_time = (time.time() - start_time) * 1000
            state.metadata.update({
                f"{agent_type.value}_latency_ms": processing_time,
                **response.metadata
            })
            
            # 완료 표시
            state.is_complete = True
            
            logger.info(f"{emoji} {agent_type.value} agent completed in {processing_time:.2f}ms")
            
            return {
                "response": state.response,
                "intermediate_responses": state.intermediate_responses,
                "reasoning": state.reasoning,
                "metadata": state.metadata,
                "is_complete": state.is_complete
            }
            
        except Exception as e:
            logger.error(f"{emoji} Error in {agent_type.value} agent: {e}")
            
            # 에러 처리
            state.errors.append(f"{agent_type.value}: {str(e)}")
            state.retry_count += 1
            
            # 일반 에이전트로 폴백
            if agent_type != AgentType.GENERAL and state.retry_count < 3:
                state.current_agent = AgentType.GENERAL
                state.reasoning.append(f"{emoji} Fallback to General agent due to error")
            
            return {
                "errors": state.errors,
                "retry_count": state.retry_count,
                "current_agent": state.current_agent,
                "reasoning": state.reasoning
            }

    def _analyze_intent(self, query: str) -> AgentType:
        """
        간단한 의도 분석 (추후 ML 모델로 교체 가능)
        """
        query_lower = query.lower()
        
        # RAG Agent 키워드
        rag_keywords = [
            "문서", "api", "가이드", "매뉴얼", "설명서", 
            "찾아", "검색", "사용법", "방법", "어떻게"
        ]
        if any(keyword in query_lower for keyword in rag_keywords):
            return AgentType.RAG
        
        # Code Agent 키워드
        code_keywords = [
            "코드", "실행", "프로그램", "계산", "함수",
            "python", "javascript", "실행해", "계산해"
        ]
        if any(keyword in query_lower for keyword in code_keywords):
            return AgentType.CODE
        
        # Search Agent 키워드
        search_keywords = [
            "최신", "뉴스", "검색", "인터넷", "웹",
            "현재", "오늘", "요즘", "트렌드"
        ]
        if any(keyword in query_lower for keyword in search_keywords):
            return AgentType.SEARCH
        
        # 기본: General Agent
        return AgentType.GENERAL

    def should_continue(self, state: GraphState) -> str:
        """
        조건부 에지 - 다음 노드 결정
        """
        # 에러가 있고 재시도 횟수가 적으면 재시도
        if state.errors and state.retry_count < 3:
            return AgentDecision.CONTINUE.value
        
        # 완료되었으면 종료
        if state.is_complete:
            return AgentDecision.FINISH.value
        
        # 현재 에이전트에 따라 라우팅
        if state.current_agent == AgentType.RAG:
            return AgentDecision.ROUTE_TO_RAG.value
        elif state.current_agent == AgentType.CODE:
            # Code 에이전트 미구현으로 General로 폴백
            return AgentDecision.ROUTE_TO_GENERAL.value
        elif state.current_agent == AgentType.SEARCH:
            return AgentDecision.ROUTE_TO_SEARCH.value
        elif state.current_agent == AgentType.GENERAL:
            return AgentDecision.ROUTE_TO_GENERAL.value
        
        # 다중 에이전트 필요시 aggregator로
        if state.requires_multi_agent:
            return AgentDecision.AGGREGATE.value
        
        # 기본값
        return AgentDecision.FINISH.value