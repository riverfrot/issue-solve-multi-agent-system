import time
from langgraph.graph import StateGraph, END
from typing import Dict, Any

from domain.models.graph_state import GraphState, AgentDecision
from domain.models.agent import AgentType
from service.graph.nodes import LangGraphNodes
from infrastructure.llm.openai_client import OpenAIClient
from utils.logger import logger


class MultiAgentWorkflow:
    """
    LangGraph 기반 멀티 에이전트 워크플로우
    """

    def __init__(
        self,
        openai_client: OpenAIClient,
    ):
        self.nodes = LangGraphNodes(openai_client)
        self.graph = self._build_workflow()
        logger.info("Multi-Agent Workflow initialized with LangGraph")

    def _build_workflow(self) -> StateGraph:
        """
        워크플로우 그래프 구성

        Flow:
        START -> Supervisor -> Agent -> END
              -> (conditional routing)
        """
        # StateGraph 생성
        workflow = StateGraph(GraphState)

        # 노드 추가
        workflow.add_node("supervisor", self.nodes.supervisor_node)
        workflow.add_node("rag_agent", self.nodes.rag_agent_node)
        workflow.add_node("code_agent", self.nodes.code_agent_node)
        workflow.add_node("search_agent", self.nodes.search_agent_node)
        workflow.add_node("general_agent", self.nodes.general_agent_node)
        workflow.add_node("aggregator", self.nodes.aggregator_node)

        # 시작점 설정
        workflow.set_entry_point("supervisor")

        # 조건부 에지 - Supervisor에서 각 에이전트로 라우팅
        workflow.add_conditional_edges(
            "supervisor",
            self._route_to_agent,
            {
                AgentDecision.ROUTE_TO_RAG.value: "rag_agent",
                AgentDecision.ROUTE_TO_CODE.value: "code_agent",
                AgentDecision.ROUTE_TO_SEARCH.value: "search_agent",
                AgentDecision.ROUTE_TO_GENERAL.value: "general_agent",
                AgentDecision.AGGREGATE.value: "aggregator",
            },
        )

        # 각 에이전트에서 종료 또는 aggregator로 이동
        for agent in ["rag_agent", "code_agent", "search_agent", "general_agent"]:
            workflow.add_conditional_edges(
                agent,
                self._should_finish,
                {
                    AgentDecision.FINISH.value: END,
                    AgentDecision.AGGREGATE.value: "aggregator",
                    AgentDecision.CONTINUE.value: "supervisor",  # 재시도
                },
            )

        # Aggregator에서 종료
        workflow.add_edge("aggregator", END)

        return workflow.compile()

    def _route_to_agent(self, state: GraphState) -> str:
        """Supervisor 결정에 따라 에이전트 라우팅"""
        if not state.current_agent:
            return AgentDecision.ROUTE_TO_GENERAL.value

        agent_routing = {
            AgentType.RAG: AgentDecision.ROUTE_TO_RAG.value,
            AgentType.CODE: AgentDecision.ROUTE_TO_CODE.value,
            AgentType.SEARCH: AgentDecision.ROUTE_TO_SEARCH.value,
            AgentType.GENERAL: AgentDecision.ROUTE_TO_GENERAL.value,
        }

        return agent_routing.get(
            state.current_agent, AgentDecision.ROUTE_TO_GENERAL.value
        )

    def _should_finish(self, state: GraphState) -> str:
        """에이전트 완료 후 다음 단계 결정"""
        # 에러가 있고 재시도 가능하면 다시 supervisor로
        if state.errors and state.retry_count < 3:
            return AgentDecision.CONTINUE.value

        # 다중 에이전트가 필요하면 aggregator로
        if state.requires_multi_agent and len(state.intermediate_responses) > 1:
            return AgentDecision.AGGREGATE.value

        # 완료되었으면 종료
        if state.is_complete:
            return AgentDecision.FINISH.value

        # 기본값: 종료
        return AgentDecision.FINISH.value

    async def execute(
        self, query: str, session_id: str, history: list = None
    ) -> Dict[str, Any]:
        """
        워크플로우 실행
        """
        logger.info(f"🚀 Starting workflow for query: {query}")

        # 초기 상태 생성
        initial_state = GraphState(
            query=query,
            session_id=session_id,
            history=history or [],
            metadata={"workflow_version": "1.0", "start_time": time.time()},
        )

        try:
            # 워크플로우 실행
            result = await self.graph.ainvoke(initial_state.model_dump())

            # 결과 처리
            final_state = GraphState(**result)

            # 실행 시간 계산
            execution_time = time.time() - final_state.metadata.get("start_time", 0)
            final_state.metadata["execution_time_ms"] = execution_time * 1000

            logger.info(f"✅ Workflow completed in {execution_time:.2f}s")

            return {
                "response": final_state.response,
                "metadata": final_state.metadata,
                "reasoning": final_state.reasoning,
                "agent_route": final_state.agent_route,
                "success": True,
            }

        except Exception as e:
            logger.error(f"❌ Workflow execution failed: {e}")

            return {
                "response": f"죄송합니다. 처리 중 오류가 발생했습니다: {str(e)}",
                "metadata": {"error": str(e)},
                "reasoning": ["Workflow execution failed"],
                "agent_route": [],
                "success": False,
            }

    def get_workflow_info(self) -> Dict[str, Any]:
        """워크플로우 정보"""
        return {
            "name": "Multi-Agent LangGraph Workflow",
            "version": "1.0",
            "nodes": [
                "supervisor",
                "rag_agent",
                "code_agent",
                "search_agent",
                "general_agent",
                "aggregator",
            ],
            "capabilities": [
                "Intent analysis and routing",
                "Document retrieval and QA",
                "Code generation and execution",
                "Web search and information gathering",
                "General conversation",
                "Multi-agent response aggregation",
            ],
            "features": [
                "Conditional routing",
                "Error handling and fallback",
                "Response aggregation",
                "Execution tracing",
                "Retry mechanism",
            ],
        }
