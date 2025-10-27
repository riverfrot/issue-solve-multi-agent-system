from typing import Optional, List, Dict
from typing_extensions import TypedDict
from datetime import datetime
from enum import Enum


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
