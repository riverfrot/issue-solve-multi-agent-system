from langchain_core.tools import tool

@tool
def search_github_issue(repo_url: str) -> str:
    """GitHub repository의 이슈를 검색합니다."""
    return f"GitHub 저장소 {repo_url}의 이슈를 검색하고 있습니다..."

ALL_TOOLS = [search_github_issue]
