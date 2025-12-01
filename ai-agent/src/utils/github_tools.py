"""
GitHub Integration Tools for Issue Analysis (Read-Only)
이슈 분석을 위한 GitHub 통합 도구들 (읽기 전용)
"""

import os
import base64
import requests
from typing import Dict, List, Optional
from langchain_core.tools import tool


@tool 
def read_file_from_repo(repo_url: str, file_path: str, branch: str = "main") -> str:
    """
    GitHub 저장소에서 특정 파일의 내용을 읽습니다.
    
    Args:
        repo_url: GitHub 저장소 URL
        file_path: 읽을 파일 경로
        branch: 브랜치 이름 (기본값: main)
        
    Returns:
        파일 내용 또는 오류 메시지
    """
    try:
        github_token = os.getenv("GITHUB_TOKEN")
        
        # repo_url에서 owner/repo 추출
        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return "올바른 GitHub URL 형식이 아닙니다."
            
        repo_path = parts[1]
        
        # GitHub API 호출
        api_url = f"https://api.github.com/repos/{repo_path}/contents/{file_path}"
        headers = {"Accept": "application/vnd.github.v3+json"}
        
        if github_token:
            headers["Authorization"] = f"token {github_token}"
            
        params = {"ref": branch}
        
        response = requests.get(api_url, headers=headers, params=params)
        response.raise_for_status()
        
        data = response.json()
        
        # Base64 디코딩
        if data.get("encoding") == "base64":
            content = base64.b64decode(data["content"]).decode("utf-8")
            return f"📄 파일: {file_path}\n\n```\n{content}\n```"
        else:
            return f"❌ 지원되지 않는 인코딩: {data.get('encoding')}"
            
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            return f"❌ 파일을 찾을 수 없습니다: {file_path}"
        else:
            return f"❌ GitHub API 오류: {str(e)}"
    except Exception as e:
        return f"❌ 파일 읽기 중 오류 발생: {str(e)}"


@tool
def get_repository_structure(repo_url: str, path: str = "", branch: str = "main") -> str:
    """
    GitHub 저장소의 디렉토리 구조를 가져옵니다.
    
    Args:
        repo_url: GitHub 저장소 URL
        path: 조회할 경로 (기본값: 루트)
        branch: 브랜치 이름 (기본값: main)
        
    Returns:
        디렉토리 구조 정보
    """
    try:
        github_token = os.getenv("GITHUB_TOKEN")
        
        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return "올바른 GitHub URL 형식이 아닙니다."
            
        repo_path = parts[1]
        
        # GitHub API 호출
        api_url = f"https://api.github.com/repos/{repo_path}/contents/{path}"
        headers = {"Accept": "application/vnd.github.v3+json"}
        
        if github_token:
            headers["Authorization"] = f"token {github_token}"
            
        params = {"ref": branch}
        
        response = requests.get(api_url, headers=headers, params=params)
        response.raise_for_status()
        
        contents = response.json()
        
        result = f"## 📁 {repo_path} 저장소 구조 ({path or '루트'})\n\n"
        
        for item in contents:
            icon = "📁" if item["type"] == "dir" else "📄"
            result += f"{icon} {item['name']}\n"
            
        return result
        
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            return f"❌ 경로를 찾을 수 없습니다: {path}"
        else:
            return f"❌ GitHub API 오류: {str(e)}"
    except Exception as e:
        return f"❌ 저장소 구조 조회 중 오류 발생: {str(e)}"


@tool
def get_repository_info(repo_url: str) -> str:
    """
    GitHub 저장소의 기본 정보를 가져옵니다.
    
    Args:
        repo_url: GitHub 저장소 URL
        
    Returns:
        저장소 정보
    """
    try:
        github_token = os.getenv("GITHUB_TOKEN")
        
        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return "올바른 GitHub URL 형식이 아닙니다."
            
        repo_path = parts[1]
        
        # GitHub API 호출
        api_url = f"https://api.github.com/repos/{repo_path}"
        headers = {"Accept": "application/vnd.github.v3+json"}
        
        if github_token:
            headers["Authorization"] = f"token {github_token}"
        
        response = requests.get(api_url, headers=headers)
        response.raise_for_status()
        
        repo_data = response.json()
        
        result = f"""## 📊 저장소 정보: {repo_data['name']}

**🔗 URL**: {repo_data['html_url']}
**📝 설명**: {repo_data.get('description', '설명 없음')}
**🏷️ 언어**: {repo_data.get('language', '미지정')}
**⭐ Stars**: {repo_data['stargazers_count']}
**🍴 Forks**: {repo_data['forks_count']}
**📅 생성일**: {repo_data['created_at'][:10]}
**🔄 업데이트**: {repo_data['updated_at'][:10]}
**📏 크기**: {repo_data['size']} KB
**🔓 공개**: {'공개' if not repo_data['private'] else '비공개'}

**🔗 Clone URL**: {repo_data['clone_url']}
"""
        
        if repo_data.get('topics'):
            result += f"**🏷️ 토픽**: {', '.join(repo_data['topics'])}\n"
            
        return result
        
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            return f"❌ 저장소를 찾을 수 없습니다: {repo_url}"
        else:
            return f"❌ GitHub API 오류: {str(e)}"
    except Exception as e:
        return f"❌ 저장소 정보 조회 중 오류 발생: {str(e)}"


@tool
def search_code_in_repo(repo_url: str, query: str, language: str = "") -> str:
    """
    GitHub 저장소에서 코드를 검색합니다.
    
    Args:
        repo_url: GitHub 저장소 URL
        query: 검색할 코드 또는 키워드
        language: 특정 언어로 제한 (선택사항)
        
    Returns:
        검색 결과
    """
    try:
        github_token = os.getenv("GITHUB_TOKEN")
        
        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return "올바른 GitHub URL 형식이 아닙니다."
            
        repo_path = parts[1]
        
        # 검색 쿼리 구성
        search_query = f"{query} repo:{repo_path}"
        if language:
            search_query += f" language:{language}"
        
        # GitHub API 호출
        api_url = "https://api.github.com/search/code"
        headers = {"Accept": "application/vnd.github.v3+json"}
        
        if github_token:
            headers["Authorization"] = f"token {github_token}"
            
        params = {"q": search_query, "per_page": 10}
        
        response = requests.get(api_url, headers=headers, params=params)
        response.raise_for_status()
        
        data = response.json()
        
        if data['total_count'] == 0:
            return f"🔍 검색 결과가 없습니다: '{query}'"
        
        result = f"## 🔍 코드 검색 결과: '{query}' (총 {data['total_count']}개)\n\n"
        
        for item in data['items'][:10]:  # 최대 10개만 표시
            result += f"### 📄 {item['name']}\n"
            result += f"**경로**: {item['path']}\n"
            result += f"**URL**: {item['html_url']}\n"
            result += f"**점수**: {item['score']:.2f}\n\n"
            
        return result
        
    except requests.exceptions.HTTPError as e:
        return f"❌ GitHub API 오류: {str(e)}"
    except Exception as e:
        return f"❌ 코드 검색 중 오류 발생: {str(e)}"


# 읽기 전용 GitHub 도구 목록 (수정 기능 제외)
READONLY_GITHUB_TOOLS = [
    read_file_from_repo,
    get_repository_structure,
    get_repository_info,
    search_code_in_repo
]

# 호환성을 위해 GITHUB_TOOLS도 동일하게 설정
GITHUB_TOOLS = READONLY_GITHUB_TOOLS