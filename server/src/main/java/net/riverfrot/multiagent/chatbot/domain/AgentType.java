package net.riverfrot.multiagent.chatbot.domain;

public enum AgentType {
    SUPERVISOR("supervisor", "🎯", "사용자 의도 분석 및 적절한 에이전트 선택"),
    RAG("rag", "📚", "내부 기본 문서탐색"),
    CODE("code", "💻", "코드 생성 및 실행"),
    SEARCH("search", "🔍", "외부 인터넷 검색"),
    GENERAL("general", "💬", "일반 대화 및 질의응답");

    private final String code;
    private final String emoji;
    private final String description;

    AgentType(String code, String emoji, String description) {
        this.code = code;
        this.emoji = emoji;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }

    public static AgentType fromCode(String code) {
        for (AgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown agent type: " + code);
    }

    public String getEmojiDisplay() {
        return emoji;
    }
}