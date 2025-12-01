"""
Supervisor Component for Issue Analysis System
이슈 분석 시스템의 감독 컴포넌트

워커 컴포넌트들을 조율하고 관리하는 감독자 역할을 담당합니다.
"""

from typing import List, Dict, Optional, Literal
from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent
from langchain_core.messages import HumanMessage, SystemMessage
from state import EnhancedState, IssueStatus, update_state_status
from prompts import get_agent_prompt


class SupervisorAgent:
    """
    Supervisor Agent - 모든 워커 에이전트를 관리하고 조율
    
    역할:
    1. 작업 할당 및 우선순위 결정
    2. 에이전트 간 조율 및 통신
    3. 워크플로우 진행 상황 모니터링
    4. 에러 처리 및 재시도 관리
    5. 최종 품질 관리
    """
    
    def __init__(self, tools: List, model: str = "gpt-4o-mini"):
        self.model = ChatOpenAI(model=model)
        self.tools = tools
        self.available_workers = [
            "planner", "researcher", "resolver", "critic", "reporter"
        ]
        
        # Supervisor 전용 시스템 프롬프트
        self.system_prompt = self._create_supervisor_prompt()
        
        # Supervisor 에이전트 생성
        self.supervisor = create_react_agent(
            self.model,
            tools=self.tools,
            prompt=self.system_prompt
        )
    
    def _create_supervisor_prompt(self) -> str:
        """Supervisor 전용 시스템 프롬프트 생성"""
        return f"""You are the Supervisor Agent in a multi-agent issue resolution system.

<Your Role>
You are responsible for:
1. **Task Assignment**: Decide which worker agent should handle each task
2. **Progress Monitoring**: Track the overall workflow progress
3. **Quality Control**: Ensure all work meets standards before proceeding
4. **Error Management**: Handle failures and decide on retries or alternative approaches
5. **Final Decision Making**: Determine when the issue analysis is complete

<Available Worker Agents>
{', '.join(self.available_workers)}

Each worker has specific capabilities:
- **planner**: Issue analysis and strategy planning
- **researcher**: Codebase analysis and information gathering  
- **resolver**: Solution recommendations and implementation guides
- **critic**: Quality validation and feedback
- **reporter**: Final report generation

<Decision Framework>
Use this framework to make routing decisions:

1. **New Issues**: Always start with 'planner'
2. **After Planning**: Route to 'researcher' for investigation
3. **After Research**: Route to 'resolver' for solution recommendations
4. **After Resolution**: Route to 'critic' for quality review
5. **After Critique**: 
   - If approved → route to 'reporter'
   - If changes needed → route back to appropriate agent
6. **After Reporting**: Task complete → 'FINISH'

<Error Handling>
- If an agent fails, analyze the error and decide:
  - Retry with same agent (max 2 retries per agent)
  - Route to different agent if error suggests wrong assignment
  - Escalate critical errors to human operator

<Output Format>
Your response must be a JSON decision:

```json
{{
  "decision": "route_to_agent|retry|finish|escalate",
  "next_agent": "planner|researcher|resolver|critic|reporter|null",
  "reasoning": "Why you made this decision",
  "instructions": "Specific instructions for the next agent",
  "priority": "high|medium|low",
  "estimated_effort": "1-5 scale",
  "quality_gates": ["list of quality requirements"]
}}
```

<Quality Gates>
Ensure these standards before allowing progression:
- Planner: Clear strategy with specific steps
- Researcher: Comprehensive findings with evidence
- Resolver: Actionable recommendations with examples
- Critic: Thorough review with specific feedback
- Reporter: Complete analysis report

Remember: You are the orchestrator ensuring smooth, high-quality issue resolution."""

    def make_routing_decision(self, state: EnhancedState) -> Dict:
        """
        현재 상태를 기반으로 다음 에이전트 라우팅 결정
        
        Args:
            state: 현재 시스템 상태
            
        Returns:
            라우팅 결정 정보
        """
        # 상태 정보를 기반으로 supervisor에게 컨텍스트 제공
        context = self._build_context(state)
        
        # Supervisor 에이전트 실행
        messages = [
            SystemMessage(content=self.system_prompt),
            HumanMessage(content=context)
        ]
        
        result = self.supervisor.invoke({"messages": messages})
        
        # 결과 파싱
        decision = self._parse_supervisor_response(result)
        
        return decision
    
    def _build_context(self, state: EnhancedState) -> str:
        """상태 정보를 기반으로 supervisor 컨텍스트 구성"""
        context = f"""
## Current System State

**Repository**: {state['repository_name']}
**Current Status**: {state['current_status'].value}
**Current Agent**: {state['current_agent']}
**Retry Count**: {state['retry_count']}/{state['max_retries']}

## Agent Execution History
"""
        
        # 최근 에이전트 실행 이력
        for step in state["agent_steps"][-3:]:  # 최근 3개 단계
            context += f"- {step['timestamp'].strftime('%H:%M:%S')} | {step['agent_name']} | {step['step_name']}\n"
        
        # 현재 이슈 정보
        if state["current_issue"]:
            issue = state["current_issue"]
            context += f"""
## Current Issue
- **#{issue['number']}**: {issue['title']}
- **Severity**: {issue['severity']}
- **Category**: {issue['category']}
"""
        
        # 리뷰 피드백 (있는 경우)
        if state.get("review_feedback"):
            latest_feedback = state["review_feedback"][-1]
            context += f"""
## Latest Review Feedback
- **Status**: {latest_feedback['approval_status']}
- **Ready for Report**: {latest_feedback.get('ready_for_report', 'unknown')}
"""
            
            if latest_feedback.get("feedback_items"):
                context += "- **Issues Found**:\n"
                for item in latest_feedback["feedback_items"][:3]:  # 최대 3개
                    context += f"  - {item.get('category', 'unknown')}: {item.get('issue', 'no description')}\n"
        
        context += f"""
## Decision Required
Based on the current state, system history, and any feedback received, 
decide what should happen next in the workflow.

Consider:
1. Has each agent completed their work satisfactorily?
2. Are there any quality issues that need addressing?
3. Is the analysis complete and ready for final reporting?
4. Should any agent retry their work?
5. Is it time to finish the workflow?
"""
        
        return context
    
    def _parse_supervisor_response(self, result: Dict) -> Dict:
        """Supervisor 응답을 파싱하여 라우팅 결정 추출"""
        try:
            # 메시지에서 JSON 파싱 시도
            last_message = result["messages"][-1] if result.get("messages") else None
            if last_message and hasattr(last_message, 'content'):
                content = last_message.content
                
                # JSON 추출 시도
                import json
                if '{' in content and '}' in content:
                    json_start = content.find('{')
                    json_end = content.rfind('}') + 1
                    decision_json = json.loads(content[json_start:json_end])
                    
                    # 필수 필드 검증
                    required_fields = ["decision", "reasoning"]
                    if all(field in decision_json for field in required_fields):
                        return decision_json
            
            # 파싱 실패 시 기본 결정
            return self._create_default_decision()
            
        except (json.JSONDecodeError, KeyError, IndexError) as e:
            print(f"Warning: Could not parse supervisor decision: {e}")
            return self._create_default_decision()
    
    def _create_default_decision(self) -> Dict:
        """기본 라우팅 결정 생성 (파싱 실패 시)"""
        return {
            "decision": "route_to_agent",
            "next_agent": "reporter",  # 안전한 기본값
            "reasoning": "Supervisor parsing failed, routing to reporter for completion",
            "instructions": "Generate final analysis report based on available information",
            "priority": "medium",
            "estimated_effort": "3",
            "quality_gates": ["comprehensive_report"]
        }
    
    def validate_quality_gates(self, state: EnhancedState, agent_name: str) -> bool:
        """에이전트별 품질 게이트 검증"""
        quality_checks = {
            "planner": self._validate_planner_output,
            "researcher": self._validate_researcher_output,
            "resolver": self._validate_resolver_output,
            "critic": self._validate_critic_output,
            "reporter": self._validate_reporter_output
        }
        
        validator = quality_checks.get(agent_name)
        if validator:
            return validator(state)
        
        return True  # 검증기가 없으면 통과
    
    def _validate_planner_output(self, state: EnhancedState) -> bool:
        """Planner 출력 품질 검증"""
        # 해결 계획이 있는지 확인
        return state.get("resolution_plan") is not None
    
    def _validate_researcher_output(self, state: EnhancedState) -> bool:
        """Researcher 출력 품질 검증"""
        # 연구 결과가 있는지 확인
        return bool(state.get("research_findings"))
    
    def _validate_resolver_output(self, state: EnhancedState) -> bool:
        """Resolver 출력 품질 검증"""
        # 메시지가 있고 내용이 충분한지 확인
        messages = state.get("messages", [])
        return len(messages) > 0 and any(
            hasattr(msg, 'content') and len(msg.content) > 100 
            for msg in messages[-3:]  # 최근 3개 메시지 검증
        )
    
    def _validate_critic_output(self, state: EnhancedState) -> bool:
        """Critic 출력 품질 검증"""
        # 리뷰 피드백이 있는지 확인
        return bool(state.get("review_feedback"))
    
    def _validate_reporter_output(self, state: EnhancedState) -> bool:
        """Reporter 출력 품질 검증"""
        # 최종 메시지가 있는지 확인
        messages = state.get("messages", [])
        return len(messages) > 0


def create_supervisor_agent(tools: List, model: str = "gpt-4o-mini") -> SupervisorAgent:
    """Supervisor 에이전트 팩토리 함수"""
    return SupervisorAgent(tools=tools, model=model)