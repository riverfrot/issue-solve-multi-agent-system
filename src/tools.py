import os
import requests
from langchain_core.tools import tool

@tool
def search_github_issue(repo_url: str, state: str = "open", labels: str = "") -> str:
    """
    GitHub repository의 이슈를 검색합니다.

    Args:
        repo_url: GitHub 저장소 URL (예: https://github.com/owner/repo)
        state: 이슈 상태 - 'open', 'closed', 'all' 중 하나 (기본값: 'open')
        labels: 검색할 라벨 (쉼표로 구분, 예: 'bug,enhancement')

    Returns:
        검색된 이슈 목록
    """
    try:
        # GitHub API 토큰
        github_token = os.getenv("GITHUB_TOKEN")

        # repo_url에서 owner/repo 추출
        # https://github.com/owner/repo -> owner/repo
        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return (
                "올바른 GitHub URL 형식이 아닙니다. 예: https://github.com/owner/repo"
            )

        repo_path = parts[1]

        # GitHub API 호출
        api_url = f"https://api.github.com/repos/{repo_path}/issues"
        headers = {"Accept": "application/vnd.github.v3+json"}

        # 토큰이 있으면 인증 헤더 추가 (rate limit 증가)
        if github_token:
            headers["Authorization"] = f"token {github_token}"

        params = {"state": state, "per_page": 10}  # 최대 10개 이슈 반환

        if labels:
            params["labels"] = labels

        response = requests.get(api_url, headers=headers, params=params)
        response.raise_for_status()

        issues = response.json()

        if not issues:
            return f"'{repo_path}' 저장소에서 조건에 맞는 이슈를 찾을 수 없습니다."

        # 결과 포맷팅
        result = f"## {repo_path} 저장소의 이슈 ({len(issues)}개)\n\n"

        for i, issue in enumerate(issues, 1):
            result += f"{i}. **#{issue['number']}** - {issue['title']}\n"
            result += f"   - 상태: {issue['state']}\n"
            result += f"   - 작성자: {issue['user']['login']}\n"
            result += f"   - URL: {issue['html_url']}\n"

            if issue.get("labels"):
                labels_str = ", ".join([label["name"] for label in issue["labels"]])
                result += f"   - 라벨: {labels_str}\n"

            result += "\n"

        return result

    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            return f"저장소를 찾을 수 없습니다: {repo_url}"
        elif e.response.status_code == 403:
            return "API rate limit에 도달했습니다. GITHUB_TOKEN 환경변수를 설정하면 더 많은 요청을 할 수 있습니다."
        else:
            return f"GitHub API 오류: {str(e)}"
    except Exception as e:
        return f"이슈 검색 중 오류 발생: {str(e)}"


@tool
def get_github_issue_detail(repo_url: str, issue_number: int) -> str:
    """
    특정 GitHub 이슈의 상세 정보를 가져옵니다.

    Args:
        repo_url: GitHub 저장소 URL
        issue_number: 이슈 번호

    Returns:
        이슈 상세 정보
    """
    try:
        github_token = os.getenv("GITHUB_TOKEN")

        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return "올바른 GitHub URL 형식이 아닙니다."

        repo_path = parts[1]

        api_url = f"https://api.github.com/repos/{repo_path}/issues/{issue_number}"
        headers = {"Accept": "application/vnd.github.v3+json"}

        if github_token:
            headers["Authorization"] = f"token {github_token}"

        response = requests.get(api_url, headers=headers)
        response.raise_for_status()

        issue = response.json()

        result = f"## 이슈 #{issue['number']}: {issue['title']}\n\n"
        result += f"**상태**: {issue['state']}\n"
        result += f"**작성자**: {issue['user']['login']}\n"
        result += f"**생성일**: {issue['created_at']}\n"
        result += f"**URL**: {issue['html_url']}\n\n"

        if issue.get("labels"):
            labels_str = ", ".join([label["name"] for label in issue["labels"]])
            result += f"**라벨**: {labels_str}\n\n"

        result += f"**설명**:\n{issue['body'] or '설명이 없습니다.'}\n\n"
        result += f"**댓글 수**: {issue['comments']}\n"

        return result

    except Exception as e:
        return f"이슈 정보를 가져오는 중 오류 발생: {str(e)}"


# 모든 도구 목록
ALL_TOOLS = [search_github_issue, get_github_issue_detail]
