/* eslint no-param-reassign: 0 */
/* Disable no-param-reassign for vuex store file due to incompatibility */
/* See: https://github.com/vuejs-templates/webpack/issues/883 */

import Vue from 'vue';
import Vuex, { StoreOptions } from 'vuex';
import { AppState, Message, ApiResponse, User } from './types';

Vue.use(Vuex);

const store: StoreOptions<AppState> = {
  state: {
    messages: [],
    isLoading: false,
    connectionStatus: 'disconnected',
    currentAgent: 'supervisor',
    sessionId: null,
    agentStatus: '4개 전문 에이전트 대기 중',
    user: null,
  },
  mutations: {
    updateMessages(state: AppState, messages: Message[]) {
      state.messages = messages;
    },
    addMessage(state: AppState, message: Message) {
      state.messages.push(message);
    },
    updateIsLoading(state: AppState, isLoading: boolean) {
      state.isLoading = isLoading;
    },
    updateConnectionStatus(state: AppState, status: 'connected' | 'connecting' | 'disconnected') {
      state.connectionStatus = status;
    },
    updateCurrentAgent(state: AppState, agent: string) {
      state.currentAgent = agent;
    },
    updateSessionId(state: AppState, sessionId: string | null) {
      state.sessionId = sessionId;
    },
    updateAgentStatus(state: AppState, status: string) {
      state.agentStatus = status;
    },
    updateStreamingMessage(state: AppState, payload: { id: number | string; content?: string; streaming?: boolean; error?: boolean }) {
      const messageIndex = state.messages.findIndex(msg => msg.id === payload.id);
      if (messageIndex !== -1) {
        if (payload.content !== undefined) {
          state.messages[messageIndex].content = payload.content;
        }
        if (payload.streaming !== undefined) {
          state.messages[messageIndex].streaming = payload.streaming;
        }
        if (payload.error !== undefined) {
          state.messages[messageIndex].error = payload.error;
        }
      }
    },
    updateUser(state: AppState, user: User) {
      state.user = user;
      // Save to localStorage
      localStorage.setItem('user', JSON.stringify(user));
    },
    clearUser(state: AppState) {
      state.user = null;
      // Remove from localStorage
      localStorage.removeItem('user');
    },
    loadUserFromStorage(state: AppState) {
      try {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          state.user = JSON.parse(storedUser);
        }
      } catch (error) {
        console.error('Failed to load user from localStorage:', error);
        localStorage.removeItem('user');
      }
    },
    clearMessages(state: AppState) {
      state.messages = [];
    },
  },
  getters: {
    messages: (state: AppState) => state.messages,
    isLoading: (state: AppState) => state.isLoading,
    connectionStatus: (state: AppState) => state.connectionStatus,
    currentAgent: (state: AppState) => state.currentAgent,
    sessionId: (state: AppState) => state.sessionId,
    agentStatus: (state: AppState) => state.agentStatus,
    user: (state: AppState) => state.user,
    isUserLoggedIn: (state: AppState) => state.user !== null,
  },
  actions: {
    addUserMessage({ commit }, message: string): Message {
      const userMessage: Message = {
        id: Date.now(),
        role: 'user',
        content: message,
        timestamp: new Date(),
        agent_type: 'user',
      };
      commit('addMessage', userMessage);
      return userMessage;
    },
    addAssistantMessage({ commit }, response: ApiResponse): Message {
      const assistantMessage: Message = {
        id: Date.now() + 1,
        role: 'assistant',
        content: response.message,
        timestamp: new Date(),
        agent_type: response.agent_used,
        metadata: response.metadata,
        thinking_process: response.thinking_process,
      };
      commit('addMessage', assistantMessage);
      return assistantMessage;
    },
    addErrorMessage({ commit }, error: Error): Message {
      const errorMessage: Message = {
        id: Date.now() + 1,
        role: 'assistant',
        content: `죄송합니다. 오류가 발생했습니다: ${error.message}`,
        timestamp: new Date(),
        agent_type: 'error',
        error: true,
      };
      commit('addMessage', errorMessage);
      return errorMessage;
    },
    async loadChatHistory({ commit }, sessionId: string) {
      try {
        const apiService = await import('@/services/ApiService');
        const historyData = await apiService.default.getChatHistory(sessionId);
        
        // Convert server response to client Message format
        const messages: Message[] = historyData.map((item: any) => ({
          id: item.id,
          role: item.role,
          content: item.content,
          timestamp: new Date(item.timestamp),
          agent_type: item.agentType || item.role === 'user' ? 'user' : 'general',
        }));
        
        commit('updateMessages', messages);
        return messages;
      } catch (error: any) {
        console.error('Failed to load chat history:', error);
        throw new Error(`채팅 히스토리 로드 실패: ${error.message}`);
      }
    },
    clearChat({ commit }) {
      // Clear all messages from UI only
      commit('clearMessages');
    },
    logout({ commit }) {
      // Clear all user data
      commit('clearUser');
      // Clear all messages
      commit('updateMessages', []);
      // Reset session
      commit('updateSessionId', null);
      // Reset loading state
      commit('updateIsLoading', false);
      // Reset connection status
      commit('updateConnectionStatus', 'disconnected');
    },
  },
};

export default new Vuex.Store(store);