from typing import Optional, List, Dict, Annotated
from typing_extensions import TypedDict
from datetime import datetime
from enum import Enum
from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages


class IssueStatus(Enum):
    """이슈 관련 상태정의"""

    DISCOVERED = "discovered"
    ANALYZING = "analyzing"
    PLANNING = "planning"
    RESEARCHING = "researching"
    IMPLEMENTING = "implemting"
    REVIEWING = "reviewing"
    RESOLVED = "resolved"
    FAILED = "failed"


class AgentStep(TypedDict):
    agent_name: str
    step_name: str
    timestamp: datetime
    result: Optional[str]
    artifacts: List[str]
    next_action: Optional[str]


class IssueInfo(TypedDict):
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
    approach: str
    steps: List[str]
    estimated_effort: str
    priority: int
    risks: List[str]
    affected_files: List[str]


class ImplementationResult(TypedDict):
    branch_name: str
    modified_files: List[str]
    new_files: List[str]
    commit_messages: List[str]
    pull_request_url: Optional[str]
    test_results: Dict[str, bool]


class ReviewFeedback(TypedDict):
    reviewer: str
    approval_status: str  # approved, changes_requested, commented
    feedback_items: List[Dict[str, str]]  # category, severity, issue, suggestion
    overall_score: int
    security_concerns: List[str]


class State(TypedDict):
    message: Annotated[List[BaseMessage], add_messages]
    repository_url: str
    repository_name: str
    target_branch: str
    current_issue: Optional[IssueInfo]
    discovered_issues: List[IssueInfo]

    current_status: IssueStatus
    current_agent: str
    agent_steps: List[AgentStep]

    resolution_plan: Optional[ResolutionPlan]
    research_findings: Dict[str, str]

    implementation_result: Optional[ImplementationResult]
    review_feedback: List[ReviewFeedback]


def create_initial_state(repository_url: str) -> State:
    repo_name = (
        repository_url.split("/")[-1] if "/" in repository_url else repository_url
    )

    return State(
        messages=[],
        repository_url=repository_url,
        repository_name=repo_name,
        target_branch="main",
        current_issue=None,
        discovered_issues=[],
        current_status=IssueStatus.DISCOVERED,
        current_agent="planner",
        agent_steps=[],
        resolution_plan=None,
        research_findings={},
        implementation_result=None,
        review_feedback=[],
    )


def update_state_status(
    state: State, new_status: IssueStatus, agent_name: str, step_result: str
):
    state["current_status"] = new_status
    state["current_agent"] = agent_name
    state["last_updated"] = datetime.now()

    step = AgentStep(
        agent_name=agent_name,
        step_name=new_status.value,
        timestamp=datetime.now(),
        result=step_result,
        artifacts=[],
        next_action=None,
    )

    state["agent_steps"].append(step)
