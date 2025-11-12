import Vue from 'vue';
import VueI18n from 'vue-i18n';
import VueCookies from 'vue-cookies';

Vue.use(VueI18n);

const messages: VueI18n.LocaleMessages = {
  ko: {
    nav: {
      title: '멀티 에이전트 챗봇',
      subtitle: '4개 전문 에이전트 시스템',
    },
    chat: {
      placeholder: '메시지를 입력하세요...',
      placeholder_disabled: 'AI가 응답 중입니다...',
      send: '전송',
      loading: '처리 중...',
      error: '오류가 발생했습니다',
      welcome: '안녕하세요! 멀티 에이전트 챗봇 시스템에 오신 것을 환영합니다!',
    },
    agents: {
      supervisor: 'Supervisor',
      rag: 'RAG Agent',
      code: 'Code Agent',
      search: 'Search Agent',
      general: 'General Agent',
      user: 'User',
      error: 'Error',
    },
    actions: {
      document_search: '문서 검색',
      web_search: '웹 검색',
      general_chat: '일반 대화',
    },
    status: {
      connected: 'connected',
      connecting: 'connecting',
      disconnected: 'disconnected',
    },
  },
  en: {
    nav: {
      title: 'Multi-Agent Chatbot',
      subtitle: '4 Specialized Agent System',
    },
    chat: {
      placeholder: 'Type your message...',
      placeholder_disabled: 'AI is responding...',
      send: 'Send',
      loading: 'Processing...',
      error: 'An error occurred',
      welcome: 'Welcome to the Multi-Agent Chatbot System!',
    },
    agents: {
      supervisor: 'Supervisor',
      rag: 'RAG Agent',
      code: 'Code Agent',
      search: 'Search Agent',
      general: 'General Agent',
      user: 'User',
      error: 'Error',
    },
    actions: {
      document_search: 'Document Search',
      code_execution: 'Code Execution',
      web_search: 'Web Search',
      general_chat: 'General Chat',
    },
    status: {
      connected: 'connected',
      connecting: 'connecting',
      disconnected: 'disconnected',
    },
  },
};

const i18n = new VueI18n({
  locale: VueCookies.get('language') || 'ko',
  fallbackLocale: 'ko',
  messages,
});

export default i18n;