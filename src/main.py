import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

from langchain_openai import ChatOpenAI
from typing import Annotated
from typing_extensions import TypedDict
from langgraph.graph.message import add_messages
from langgraph.prebuilt import create_react_agent
from langchain_core.messages import HumanMessage

from langgraph.graph import END, StateGraph, START
from langchain_core.runnables import RunnableConfig
from prompts import get_agent_prompt
from tools import ALL_TOOLS


# State 정의
class State(TypedDict):
    # list 타입에 add_messages 적용(list 에 message 추가)
    messages: Annotated[list, add_messages]


# Planner 에이전트 생성
planner = create_react_agent(
    ChatOpenAI(model="gpt-4"), tools=ALL_TOOLS, prompt=get_agent_prompt("planner")
)

# Researcher 에이전트 생성
researcher = create_react_agent(
    ChatOpenAI(model="gpt-4"), tools=ALL_TOOLS, prompt=get_agent_prompt("researcher")
)

# Resolver 에이전트 생성
resolver = create_react_agent(
    ChatOpenAI(model="gpt-4"), tools=ALL_TOOLS, prompt=get_agent_prompt("resolver")
)

# Critic 에이전트 생성
critic = create_react_agent(
    ChatOpenAI(model="gpt-4"), tools=ALL_TOOLS, prompt=get_agent_prompt("critic")
)

# Reporter 에이전트 생성
reporter = create_react_agent(
    ChatOpenAI(model="gpt-4"), tools=ALL_TOOLS, prompt=get_agent_prompt("reporter")
)

# 그래프 생성
workflow = StateGraph(State)

# 그래프에 노드 추가
workflow.add_node("Planner", planner)
workflow.add_node("Researcher", researcher)
workflow.add_node("Resolver", resolver)
workflow.add_node("Critic", critic)
workflow.add_node("Reporter", reporter)

# 시작점
workflow.add_edge(START, "Planner")
workflow.add_edge("Planner", "Researcher")
workflow.add_edge("Researcher", "Resolver")
workflow.add_edge("Resolver", "Critic")
workflow.add_edge("Critic", "Reporter")
workflow.add_edge("Reporter", END)


# 그래프 컴파일
graph = workflow.compile()

# config 설정(재귀 최대 횟수, thread_id)
config = RunnableConfig(recursion_limit=10)

# 질문 입력
inputs = {
    "messages": [
        HumanMessage(
            content="https://github.com/riverfrot/sample-spring 여기에 있는 이슈를 분석해주세요"
        )
    ],
}

# 그래프 실행
if __name__ == "__main__":
    result = graph.invoke(inputs, config)
    # 결과 출력
    for message in result["messages"]:
        if hasattr(message, "content"):
            print(f"message : {message.content}")
            print("-------------------------")
