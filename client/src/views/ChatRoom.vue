<template>
  <b-container fluid class="chat-room">
    <b-row class="h-100">
      <b-col cols="12" class="d-flex flex-column">
        <!-- Messages Area -->
        <div 
          ref="messagesContainer"
          class="messages-container flex-grow-1"
          @scroll="handleScroll"
        >
          <b-card
            v-for="message in messages"
            :key="message.id"
            :class="[
              'message-card',
              message.role === 'user' ? 'user-message' : 'assistant-message',
              { 'error-message': message.error }
            ]"
            no-body
          >
            <b-card-body class="py-2 px-3">
              <div class="d-flex align-items-start">
                <div class="message-avatar mr-2">
                  {{ getAgentEmoji(message.agent_type) }}
                </div>
                <div class="flex-grow-1">
                  <div class="message-header d-flex justify-content-between align-items-center">
                    <small class="text-muted">{{ getAgentName(message.agent_type) }}</small>
                    <small class="text-muted">{{ formatTime(message.timestamp) }}</small>
                  </div>
                  <div class="message-content">{{ message.content }}</div>
                  
                  <!-- Metadata Display -->
                  <div v-if="message.metadata && showMetadata" class="metadata mt-2">
                    <b-badge variant="secondary" class="mr-1">
                      처리시간: {{ message.metadata.processing_time }}ms
                    </b-badge>
                  </div>
                </div>
              </div>
            </b-card-body>
          </b-card>
          
          <!-- Loading Indicator -->
          <div v-if="isLoading" class="loading-indicator text-center py-3">
            <b-spinner variant="primary" class="mr-2" />
            <span>{{ getAgentName(currentAgent) }}가 처리 중입니다...</span>
          </div>
        </div>

        <!-- Input Area -->
        <chat-input 
          @send-message="handleSendMessage" 
          :disabled="isLoading" 
        />
        
        <!-- Quick Actions -->
        <div class="quick-actions mt-2 d-flex flex-wrap justify-content-center">
          <b-button
            v-for="action in quickActions"
            :key="action.label"
            @click="sendQuickAction(action)"
            :disabled="isLoading"
            variant="outline-primary"
            size="sm"
            class="m-1"
          >
            {{ action.emoji }} {{ action.label }}
          </b-button>
        </div>
      </b-col>
    </b-row>

  </b-container>
</template>

<script>
import { mapState, mapActions, mapGetters } from 'vuex';
import ChatInput from '@/components/ChatInput.vue';
import apiService from '@/services/ApiService';

export default {
  name: 'ChatRoom',
  components: {
    ChatInput,
  },
  props: {
    sessionId: {
      type: String,
      default: null,
    },
  },
  data() {
    return {
      isUserScrolled: false,
      showMetadata: false,
      streamingMessageId: null,
      streamingContent: '',
      quickActions: [
        {
          emoji: '📚',
          label: '문서 검색',
          text: '내부 문서에서 API 사용법을 찾아줘',
        },
        {
          emoji: '🔍',
          label: '웹 검색',
          text: '최신 AI 기술 뉴스를 검색해줘',
        },
        {
          emoji: '💬',
          label: '일반 대화',
          text: 'AI에 대해 간단히 설명해줘',
        },
      ],
    };
  },
  computed: {
    ...mapState(['messages', 'isLoading', 'currentAgent', 'user']),
    ...mapGetters(['isUserLoggedIn']),
    storeSessionId() {
      return this.$store.state.sessionId;
    },
    currentSessionId() {
      return this.sessionId || this.storeSessionId || this.generateSessionId();
    },
  },
  watch: {
    messages: {
      handler() {
        this.scrollToBottom();
      },
      deep: true,
    },
    isLoading() {
      this.scrollToBottom();
    },
    // 세션 ID 변경 시 자동으로 채팅 클리어 후 히스토리 로드
    sessionId: {
      handler(newSessionId, oldSessionId) {
        if (newSessionId && newSessionId !== oldSessionId) {
          this.handleSessionChange(newSessionId);
        }
      },
      immediate: false, // mounted에서 처리하므로 immediate 불필요
    },
    currentSessionId: {
      handler(newSessionId, oldSessionId) {
        if (newSessionId && newSessionId !== oldSessionId) {
          this.handleSessionChange(newSessionId);
        }
      },
      immediate: false,
    },
  },
  methods: {
    ...mapActions(['addUserMessage', 'addAssistantMessage', 'addErrorMessage', 'loadChatHistory', 'clearChat']),
    async handleSendMessage(message) {
      if (this.isLoading) return;

      this.addUserMessage(message);
      this.$store.commit('updateIsLoading', true);
      this.$store.commit('updateCurrentAgent', 'supervisor');
      this.$store.commit('updateAgentStatus', '🎯 Supervisor가 의도를 분석하고 있습니다...');

      this.isUserScrolled = false;

      // Create streaming message placeholder
      const streamingMessage = {
        id: 'streaming_' + Date.now(),
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        agent_type: 'supervisor',
        streaming: true
      };

      this.$store.commit('addMessage', streamingMessage);
      this.streamingMessageId = streamingMessage.id;
      this.streamingContent = '';

      try {
        await apiService.sendStreamingMessage(
          message,
          this.currentSessionId,
          this.getCurrentUserId(), // 기본 사용자 ID 추가
          (chunk) => {
            // Update streaming content
            this.streamingContent += chunk;
            
            // Update the streaming message in store
            this.$store.commit('updateStreamingMessage', {
              id: this.streamingMessageId,
              content: this.streamingContent
            });
            
            this.$store.commit('updateAgentStatus', '🎯 AI가 응답하고 있습니다...');
          },
          () => {
            // Streaming completed
            this.$store.commit('updateStreamingMessage', {
              id: this.streamingMessageId,
              streaming: false
            });
            this.$store.commit('updateCurrentAgent', 'general');
            this.$store.commit('updateAgentStatus', '✅ 응답 완료');
            this.$store.commit('updateIsLoading', false);
            
            this.streamingMessageId = null;
            this.streamingContent = '';
          },
          (error) => {
            console.error('Streaming failed:', error);
            
            // Update streaming message with error
            this.$store.commit('updateStreamingMessage', {
              id: this.streamingMessageId,
              content: 'Sorry, an error occurred. Please try again.',
              error: true
            });
            
            this.$store.commit('updateAgentStatus', '❌ 오류 발생 - 다시 시도해주세요');
            this.$store.commit('updateIsLoading', false);
            
            this.streamingMessageId = null;
            this.streamingContent = '';
          }
        );
        
      } catch (error) {
        console.error('Message send failed:', error);
        this.addErrorMessage(error);
        this.$store.commit('updateAgentStatus', '❌ 오류 발생 - 다시 시도해주세요');
        this.$store.commit('updateIsLoading', false);
        
        this.streamingMessageId = null;
        this.streamingContent = '';
      }
    },
    sendQuickAction(action) {
      this.handleSendMessage(action.text);
    },
    scrollToBottom() {
      if (this.$refs.messagesContainer && !this.isUserScrolled) {
        this.$nextTick(() => {
          const container = this.$refs.messagesContainer;
          container.scrollTop = container.scrollHeight;
        });
      }
    },
    handleScroll() {
      const container = this.$refs.messagesContainer;
      if (container) {
        const { scrollTop, scrollHeight, clientHeight } = container;
        this.isUserScrolled = scrollTop < scrollHeight - clientHeight - 50;
      }
    },
    getAgentEmoji(agentType) {
      const emojis = {
        user: '👤',
        supervisor: '🎯',
        rag: '📚',
        code: '💻',
        search: '🔍',
        general: '💬',
        error: '❌',
      };
      return emojis[agentType] || '🤖';
    },
    getAgentName(agentType) {
      const names = {
        user: 'User',
        supervisor: 'Supervisor',
        rag: 'RAG Agent',
        search: 'Search Agent',
        general: 'General Agent',
        error: 'Error',
      };
      return names[agentType] || 'AI Agent';
    },
    getAgentStatusText(agentType) {
      const statusTexts = {
        supervisor: '🎯 Supervisor - 요청 분석 완료',
        rag: '📚 RAG Agent - 문서 검색 완료',
        search: '🔍 Search Agent - 웹 검색 완료',
        general: '💬 General Agent - 대화 완료',
      };
      return statusTexts[agentType] || '🤖 처리 완료';
    },
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleTimeString('ko-KR', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
      });
    },
    generateSessionId() {
      return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    },
    getCurrentUserId() {
      return this.user?.id || 'user_default';
    },
    addWelcomeMessage(nickname) {
      const welcomeMessage = {
        id: Date.now(),
        role: 'assistant',
        content: `안녕하세요 ${nickname}님! 👋\n\n멀티 에이전트 챗봇 시스템에 오신 것을 환영합니다!\n\n저는 4개의 전문 에이전트가 협력하는 AI 시스템입니다:\n- 📚 **문서 검색**: "API 사용법을 알려줘"\n- 🔍 **웹 검색**: "최신 기술 뉴스 검색"\n- 💬 **일반 대화**: "AI에 대해 설명해줘"\n\n무엇을 도와드릴까요? 🎯`,
        timestamp: new Date(),
        agent_type: 'general',
        welcome: true,
      };
      this.$store.commit('addMessage', welcomeMessage);
    },
    async loadChatHistoryFromServer() {
      if (this.isLoading) return;
      
      try {
        this.$store.commit('updateIsLoading', true);
        await this.loadChatHistory(this.currentSessionId);
        this.$store.commit('updateAgentStatus', '✅ 채팅 히스토리 로드 완료');
      } catch (error) {
        console.error('Failed to load chat history:', error);
        this.$store.commit('updateAgentStatus', '❌ 히스토리 로드 실패');
        this.addErrorMessage(error);
      } finally {
        this.$store.commit('updateIsLoading', false);
      }
    },
    clearChatHistory() {
      if (this.isLoading) return;
      
      this.clearChat();
      this.$store.commit('updateAgentStatus', '🗑️ 채팅 기록이 클리어되었습니다');
      
      // 웰컴 메시지 다시 추가
      if (this.isUserLoggedIn) {
        this.addWelcomeMessage(this.user.nickname);
      }
    },
    async handleSessionChange(newSessionId) {
      if (!this.isUserLoggedIn || this.isLoading) return;
      
      try {
        // 1단계: 채팅 클리어
        this.clearChat();
        
        // 2단계: 세션 ID 업데이트
        this.$store.commit('updateSessionId', newSessionId);
        
        // 3단계: 히스토리 로드 시도
        const history = await this.loadChatHistory(newSessionId);
        
        // 4단계: 히스토리가 없으면 웰컴 메시지 추가
        if (!history || history.length === 0) {
          this.addWelcomeMessage(this.user.nickname);
        }
        
        this.$store.commit('updateAgentStatus', `📝 ${newSessionId.substring(0, 8)}... 채팅방으로 이동완료`);
        
      } catch (error) {
        // 히스토리 로드 실패시 웰컴 메시지 표시
        this.addWelcomeMessage(this.user.nickname);
        this.$store.commit('updateAgentStatus', '✨ 새로운 채팅방입니다');
      }
    },
  },
  async mounted() {
    // Initialize session or use existing
    if (this.sessionId) {
      this.$store.commit('updateSessionId', this.sessionId);
    }

    // Use the same logic as session change for initial load
    if (this.isUserLoggedIn) {
      await this.handleSessionChange(this.currentSessionId);
    } else {
      // If user is not logged in, add welcome message for anonymous usage
      if (this.messages.length === 0) {
        this.addWelcomeMessage('Guest');
      }
    }
  },
};
</script>

<style lang="scss" scoped>
// Variables are now globally imported via vite.config.js

.chat-room {
  height: calc(100vh - 120px);
}

.messages-container {
  max-height: 70vh;
  overflow-y: auto;
  padding: 1rem;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  background-color: #f8f9fa;
}

.message-card {
  margin-bottom: 0.5rem;
  border: none;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
  
  &.user-message {
    margin-left: auto;
    max-width: 70%;
    background-color: #007bff;
    color: white;
    
    .message-header,
    .message-content {
      color: white;
    }
  }
  
  &.assistant-message {
    margin-right: auto;
    max-width: 70%;
    background-color: #ffffff;
  }
  
  &.error-message {
    background-color: #f8d7da;
    border-color: #f5c6cb;
    color: #721c24;
  }
}

.message-avatar {
  font-size: 1.2rem;
  line-height: 1;
}

.message-content {
  font-size: 0.9rem;
  line-height: 1.4;
  white-space: pre-wrap;
}

.loading-indicator {
  color: #6c757d;
  font-size: 0.9rem;
}

.chat-controls {
  padding: 0.25rem 0;
  border-top: 1px solid #e9ecef;
  margin-top: 0.5rem;
  
  .btn {
    font-size: 0.8rem;
    padding: 0.25rem 0.5rem;
  }
}

.quick-actions {
  padding: 0.5rem 0;
}

.metadata {
  font-size: 0.75rem;
}
</style>