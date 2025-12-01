"""
Enhanced State Management for Issue Resolution Multi-Agent System
이슈 해결 멀티 에이전트 시스템을 위한 향상된 상태 관리
"""

from typing import List, Dict, Optional, Annotated, TypedDict
from typing_extensions import TypedDict
from langgraph.graph.message import add_messages
from langchain_core.messages import BaseMessage
from datetime import datetime
from enum import Enum


class IssueStatus(Enum):
    """이슈 처리 상태"""
    DISCOVERED = "discovered"
    ANALYZING = "analyzing"
    PLANNING = "planning"
    RESEARCHING = "researching"
    IMPLEMENTING = "implementing"
    REVIEWING = "reviewing"
    RESOLVED = "resolved"
    FAILED = "failed"


class AgentStep(TypedDict):
    """에이전트 단계 정보"""
    agent_name: str
    step_name: str
    timestamp: datetime
    result: Optional[str]
    artifacts: List[str]  # 생성된 파일, 코드 등
    next_action: Optional[str]


class IssueInfo(TypedDict):
    """이슈 정보"""
    number: int
    title: str
    url: str
    status: IssueStatus
    severity: str  # critical, high, medium, low
    category: str  # bug, feature, enhancement, etc.
    description: str
    assignee: Optional[str]
    created_at: str
    updated_at: str


class ResolutionPlan(TypedDict):
    """해결 계획"""
    approach: str
    steps: List[str]
    estimated_effort: str
    priority: int
    risks: List[str]
    affected_files: List[str]


class ImplementationResult(TypedDict):
    """구현 결과"""
    branch_name: str
    modified_files: List[str]
    new_files: List[str]
    commit_messages: List[str]
    pull_request_url: Optional[str]
    test_results: Dict[str, bool]


class ReviewFeedback(TypedDict):
    """리뷰 피드백"""
    reviewer: str
    approval_status: str  # approved, changes_requested, commented
    feedback_items: List[Dict[str, str]]  # category, severity, issue, suggestion
    overall_score: int
    security_concerns: List[str]


class EnhancedState(TypedDict):
    """향상된 상태 정의"""
    # 기본 메시지 (기존과 동일)
    messages: Annotated[List[BaseMessage], add_messages]
    
    # 저장소 정보
    repository_url: str
    repository_name: str
    target_branch: str
    
    # 이슈 관련 정보
    current_issue: Optional[IssueInfo]
    discovered_issues: List[IssueInfo]
    
    # 처리 진행 상황
    current_status: IssueStatus
    current_agent: str
    agent_steps: List[AgentStep]
    
    # Supervisor 관련 필드 추가
    next_agent: Optional[str]  # Supervisor가 결정한 다음 에이전트
    
    # 계획 및 분석
    resolution_plan: Optional[ResolutionPlan]
    research_findings: Dict[str, str]  # 연구 결과
    
    # 구현 및 결과
    implementation_result: Optional[ImplementationResult]
    review_feedback: List[ReviewFeedback]
    
    # 추적 정보
    session_id: str
    started_at: datetime
    last_updated: datetime
    retry_count: int
    max_retries: int
    
    # 설정
    auto_create_pr: bool
    auto_assign_reviewers: bool
    require_tests: bool


def create_initial_state(repository_url: str, session_id: str = None) -> EnhancedState:
    """초기 상태 생성"""
    if not session_id:
        session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    
    repo_name = repository_url.split('/')[-1] if '/' in repository_url else repository_url
    
    return EnhancedState(
        messages=[],
        repository_url=repository_url,
        repository_name=repo_name,
        target_branch="main",
        current_issue=None,
        discovered_issues=[],
        current_status=IssueStatus.DISCOVERED,
        current_agent="supervisor",
        agent_steps=[],
        next_agent=None,  # Supervisor 관련 필드 초기화
        resolution_plan=None,
        research_findings={},
        implementation_result=None,
        review_feedback=[],
        session_id=session_id,
        started_at=datetime.now(),
        last_updated=datetime.now(),
        retry_count=0,
        max_retries=3,
        auto_create_pr=True,
        auto_assign_reviewers=False,
        require_tests=True
    )


def update_state_status(state: EnhancedState, new_status: IssueStatus, 
                       agent_name: str, step_result: str = None) -> None:
    """상태 업데이트"""
    state["current_status"] = new_status
    state["current_agent"] = agent_name
    state["last_updated"] = datetime.now()
    
    # 에이전트 단계 추가
    step = AgentStep(
        agent_name=agent_name,
        step_name=new_status.value,
        timestamp=datetime.now(),
        result=step_result,
        artifacts=[],
        next_action=None
    )
    state["agent_steps"].append(step)


def add_issue_to_state(state: EnhancedState, issue_data: Dict) -> None:
    """이슈를 상태에 추가"""
    issue = IssueInfo(
        number=issue_data.get("number", 0),
        title=issue_data.get("title", ""),
        url=issue_data.get("html_url", ""),
        status=IssueStatus.DISCOVERED,
        severity=issue_data.get("severity", "medium"),
        category=issue_data.get("category", "bug"),
        description=issue_data.get("body", ""),
        assignee=None,
        created_at=issue_data.get("created_at", ""),
        updated_at=issue_data.get("updated_at", "")
    )
    
    state["discovered_issues"].append(issue)
    if not state["current_issue"]:
        state["current_issue"] = issue


def get_state_summary(state: EnhancedState) -> str:
    """상태 요약 반환"""
    retry_info = f"{state['retry_count']}/{state['max_retries']}"
    
    summary = f"""
## 🔄 세션 상태 요약
- **세션 ID**: {state['session_id']}
- **저장소**: {state['repository_name']}
- **현재 상태**: {state['current_status'].value}
- **현재 에이전트**: {state['current_agent']}
- **발견된 이슈**: {len(state['discovered_issues'])}개
- **재시도 횟수**: {retry_info}

## 📝 처리 진행상황
"""
    for step in state["agent_steps"][-5:]:  # 최근 5개 단계만 표시
        timestamp_str = step['timestamp'].strftime('%H:%M:%S')
        summary += f"- {timestamp_str} | {step['agent_name']} | {step['step_name']}\n"
    
    if state["current_issue"]:
        issue_num = state['current_issue']['number']
        issue_title = state['current_issue']['title']
        issue_severity = state['current_issue']['severity']
        issue_category = state['current_issue']['category']
        
        summary += f"\n## 🎯 현재 처리 중인 이슈\n"
        summary += f"- **#{issue_num}**: {issue_title}\n"
        summary += f"- **심각도**: {issue_severity}\n"
        summary += f"- **카테고리**: {issue_category}\n"
    
    return summary