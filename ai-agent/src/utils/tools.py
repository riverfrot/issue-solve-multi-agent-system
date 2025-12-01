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
            return f"🔍 검색 결과: '{state}' 상태의 이슈가 없습니다."

        result = f"## 📋 GitHub 이슈 목록 ({len(issues)}개)\n\n"

        for issue in issues:
            # Pull Request는 제외 (이슈만 포함)
            if "pull_request" not in issue:
                result += f"### #{issue['number']} {issue['title']}\n"
                result += f"**상태**: {issue['state']}\n"
                result += f"**작성자**: {issue['user']['login']}\n"
                result += f"**생성일**: {issue['created_at'][:10]}\n"

                # 라벨 정보 추가
                if issue["labels"]:
                    labels_str = ", ".join([label["name"] for label in issue["labels"]])
                    result += f"**라벨**: {labels_str}\n"

                # 이슈 내용 요약 (첫 100자)
                body = issue["body"] or ""
                if len(body) > 100:
                    body = body[:100] + "..."
                result += f"**설명**: {body}\n"
                result += f"**URL**: {issue['html_url']}\n\n"
                result += "---\n\n"

        return result

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

        # repo_url에서 owner/repo 추출
        parts = repo_url.rstrip("/").split("github.com/")
        if len(parts) < 2:
            return "올바른 GitHub URL 형식이 아닙니다."

        repo_path = parts[1]

        # GitHub API 호출
        api_url = f"https://api.github.com/repos/{repo_path}/issues/{issue_number}"
        headers = {"Accept": "application/vnd.github.v3+json"}

        if github_token:
            headers["Authorization"] = f"token {github_token}"

        response = requests.get(api_url, headers=headers)
        response.raise_for_status()

        issue = response.json()

        result = f"## 📄 이슈 #{issue['number']}: {issue['title']}\n\n"
        result += f"**상태**: {issue['state']}\n"
        result += f"**작성자**: {issue['user']['login']}\n"
        result += f"**생성일**: {issue['created_at']}\n"
        result += f"**수정일**: {issue['updated_at']}\n"
        result += f"**URL**: {issue['html_url']}\n\n"

        # 라벨 정보
        if issue["labels"]:
            labels_str = ", ".join([label["name"] for label in issue["labels"]])
            result += f"**라벨**: {labels_str}\n\n"

        result += f"**설명**:\n{issue['body'] or '설명이 없습니다.'}\n\n"
        result += f"**댓글 수**: {issue['comments']}\n"

        return result

    except Exception as e:
        return f"이슈 정보를 가져오는 중 오류 발생: {str(e)}"


@tool
def generate_test_code(test_type: str, language: str, framework: str, test_scenario: str, 
                      class_or_function_name: str = "", expected_behavior: str = "") -> str:
    """
    다양한 언어와 프레임워크에 대한 테스트 코드를 생성합니다.
    
    Args:
        test_type: 테스트 유형 ('unit', 'integration', 'e2e')
        language: 프로그래밍 언어 ('java', 'javascript', 'python', 'typescript')
        framework: 테스트 프레임워크 ('junit', 'jest', 'pytest', 'spring-boot-test')
        test_scenario: 테스트 시나리오 설명
        class_or_function_name: 테스트할 클래스나 함수 이름 (선택)
        expected_behavior: 예상 동작 설명 (선택)
        
    Returns:
        생성된 테스트 코드
    """
    
    test_templates = {
        'java': {
            'junit': {
                'unit': '''@Test
public void test{test_name}() {{
    // Given
    {given_section}
    
    // When
    {when_section}
    
    // Then
    {then_section}
}}''',
                'integration': '''@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class {class_name}IntegrationTest {{
    
    @Autowired
    private {service_name} {service_var};
    
    @Test
    void test{test_name}() {{
        // Given
        {given_section}
        
        // When
        {when_section}
        
        // Then
        {then_section}
    }}
}}'''
            }
        },
        'python': {
            'pytest': {
                'unit': '''def test_{test_name}():
    # Given
    {given_section}
    
    # When
    {when_section}
    
    # Then
    {then_section}''',
                'integration': '''@pytest.mark.integration
def test_{test_name}():
    # Given
    {given_section}
    
    # When  
    {when_section}
    
    # Then
    {then_section}'''
            }
        },
        'javascript': {
            'jest': {
                'unit': '''test('{test_description}', () => {{
    // Given
    {given_section}
    
    // When
    {when_section}
    
    // Then
    {then_section}
}});''',
                'integration': '''describe('{class_name} Integration', () => {{
    test('{test_description}', async () => {{
        // Given
        {given_section}
        
        // When
        {when_section}
        
        // Then
        {then_section}
    }});
}});'''
            }
        }
    }
    
    # 테스트 이름 생성
    test_name = class_or_function_name or test_scenario.replace(' ', '_').lower()
    
    # 템플릿 선택
    template = test_templates.get(language, {}).get(framework, {}).get(test_type, '')
    
    if not template:
        return f"⚠️ 지원되지 않는 조합입니다: {language}/{framework}/{test_type}"
    
    # 기본 섹션 생성
    given_section = f"// TODO: {test_scenario}을 위한 테스트 데이터 준비"
    when_section = f"// TODO: {class_or_function_name or '테스트 대상'} 실행"
    then_section = f"// TODO: {expected_behavior or '예상 결과'} 검증"
    
    # 언어별 주석 스타일 조정
    if language == 'python':
        given_section = given_section.replace('//', '#')
        when_section = when_section.replace('//', '#')  
        then_section = then_section.replace('//', '#')
    
    # 템플릿 변수 치환
    test_code = template.format(
        test_name=test_name.title().replace('_', ''),
        class_name=class_or_function_name or 'TestClass',
        service_name=class_or_function_name or 'TestService',
        service_var=(class_or_function_name or 'testService').lower(),
        test_description=test_scenario,
        given_section=given_section,
        when_section=when_section,
        then_section=then_section
    )
    
    return f"""## 🧪 생성된 테스트 코드

**테스트 유형**: {test_type}
**언어/프레임워크**: {language}/{framework}
**시나리오**: {test_scenario}

```{language}
{test_code}
```

💡 **사용법**: 위 코드를 적절한 테스트 파일에 복사하고 실제 구현에 맞게 수정하세요."""


@tool
def create_issue_analysis_report(issues_data: str, repo_url: str, implementation_status: str = "analysis_only") -> str:
    """
    이슈 분석 결과를 마크다운 보고서로 생성합니다. 중복 방지 및 내용 차별화 기능 포함.
    
    Args:
        issues_data: 이슈 분석 데이터 (상세한 분석 결과)
        repo_url: GitHub 저장소 URL
        implementation_status: 구현 상태 ("analysis_only", "implementation_completed", "partial_implementation")
        
    Returns:
        생성된 마크다운 보고서 내용
    """
    from datetime import datetime
    import hashlib
    import os
    
    # 현재 시간
    current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    # 내용 해시 생성 (중복 검사용)
    content_hash = hashlib.md5((issues_data + repo_url + implementation_status).encode()).hexdigest()[:8]
    
    # 기존 보고서 파일들 확인
    try:
        existing_reports = [f for f in os.listdir('.') if f.startswith('issue_') and f.endswith('.md')]
        
        # 중복 검사
        for report_file in existing_reports:
            try:
                with open(report_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if content_hash in content:
                        return f"⚠️ 동일한 내용의 보고서가 이미 존재합니다: {report_file}\n중복 생성을 방지했습니다."
            except Exception:
                continue
    except Exception:
        pass  # 디렉토리 읽기 실패 시 무시하고 계속 진행
    
    # 저장소 이름 추출
    repo_name = repo_url.split('/')[-1] if '/' in repo_url else repo_url
    
    # 구현 상태에 따른 보고서 제목 및 내용 차별화
    status_info = {
        "analysis_only": {
            "title": "📋 이슈 분석 보고서",
            "summary": "본 보고서는 이슈 분석 및 해결 방안 제안을 목적으로 생성되었습니다.",
            "next_steps": "분석된 내용을 바탕으로 실제 구현을 진행하시기 바랍니다."
        },
        "implementation_completed": {
            "title": "✅ 이슈 해결 완료 보고서",
            "summary": "본 보고서는 실제 코드 구현 및 이슈 해결 완료 결과를 제시합니다.",
            "next_steps": "구현된 솔루션을 프로덕션에 배포하고 모니터링하시기 바랍니다."
        },
        "partial_implementation": {
            "title": "🔄 이슈 부분 해결 보고서",
            "summary": "본 보고서는 부분적으로 구현된 솔루션과 추가 작업이 필요한 항목들을 제시합니다.",
            "next_steps": "남은 구현 사항을 완료하고 종합 테스트를 진행하시기 바랍니다."
        }
    }
    
    current_status = status_info.get(implementation_status, status_info["analysis_only"])
    
    # 마크다운 보고서 생성 (상태별 차별화)
    report = f"""# 🔍 {current_status['title']}: {repo_name}

**📂 분석 대상**: {repo_url}  
**📅 분석 일시**: {current_time}  
**🤖 생성 시스템**: Multi-Agent Issue Analysis System v2.0  
**🔧 구현 상태**: {implementation_status.replace('_', ' ').title()}  
**🔑 보고서 ID**: {content_hash}  

---

## 📋 Executive Summary

{current_status['summary']} 
멀티 에이전트 시스템(Planner → Researcher → Resolver → Critic → Reporter)을 통해 각 이슈의 근본 원인을 분석하고, 
구체적인 해결 방안과 테스트 전략을 제안합니다.

---

## 🔍 상세 분석 결과

{issues_data}

---

## 📊 분석 요약 및 권장사항

### 🎯 우선순위 해결 로드맵

1. **🔴 높은 우선순위**: 보안 및 안정성 관련 이슈
   - 입력 검증 강화
   - 동시성 문제 해결
   - 예외 처리 개선

2. **🟡 중간 우선순위**: 아키텍처 개선
   - 데이터 영속성 구현
   - 로깅 및 모니터링 추가
   - 성능 최적화

3. **🟢 낮은 우선순위**: 개발 환경 및 도구
   - 컨테이너화
   - CI/CD 파이프라인
   - 문서화 개선

### 🛡️ 품질 보증 전략

- **테스트 커버리지**: 각 이슈에 대한 단위/통합 테스트 구현
- **코드 리뷰**: 보안 및 성능 관점에서의 철저한 검토
- **점진적 배포**: 피처 플래그를 활용한 단계적 출시
- **모니터링**: 실시간 이슈 감지 및 알림 체계 구축

### 📈 기대 효과

- **보안 강화**: 입력 검증 및 예외 처리를 통한 보안 취약점 해결
- **성능 향상**: 동시성 처리 개선으로 시스템 안정성 확보
- **유지보수성**: 체계적인 테스트 코드로 안전한 코드 변경 가능
- **확장성**: 데이터베이스 연동으로 대용량 데이터 처리 지원

---

## 🚀 Next Steps

{current_status['next_steps']}

### 📋 실행 계획
1. **즉시 실행**: 보안 관련 이슈 우선 해결
2. **1주 내**: 동시성 문제 해결 및 테스트 코드 작성
3. **2주 내**: 데이터베이스 연동 및 성능 테스트
4. **1개월 내**: 컨테이너화 및 CI/CD 구축

---

*🔑 보고서 ID: {content_hash}*  
*각 이슈별 상세 구현 가이드와 테스트 코드는 개발팀의 실제 환경에 맞게 조정하여 사용하시기 바랍니다.*
"""
    
    # 보고서를 파일로 저장 (상태별 파일명)
    status_prefix = {
        "analysis_only": "analysis",
        "implementation_completed": "resolved", 
        "partial_implementation": "partial"
    }.get(implementation_status, "analysis")
    
    report_filename = f"issue_{status_prefix}_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{content_hash}.md"
    
    try:
        with open(report_filename, 'w', encoding='utf-8') as f:
            f.write(report)
        
        # 성공 메시지도 상태별로 차별화
        success_messages = {
            "analysis_only": "📋 이슈 분석 보고서가 생성되었습니다",
            "implementation_completed": "✅ 이슈 해결 완료 보고서가 생성되었습니다",
            "partial_implementation": "🔄 부분 해결 보고서가 생성되었습니다"
        }
        
        success_msg = success_messages.get(implementation_status, success_messages["analysis_only"])
        return f"{success_msg}: {report_filename}\n\n{report}"
        
    except Exception as e:
        return f"❌ 보고서 생성 중 오류 발생: {str(e)}\n\n생성된 보고서 내용:\n{report}"


# 모든 도구 목록
ALL_TOOLS = [search_github_issue, get_github_issue_detail, generate_test_code, create_issue_analysis_report]