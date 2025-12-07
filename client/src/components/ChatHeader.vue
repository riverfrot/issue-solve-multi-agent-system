<template>
  <b-navbar class="chat-header" variant="dark">
    <b-navbar-brand>
      <div class="d-flex align-items-center">
        <span class="mr-2">🤖</span>
        <div>
          <h5 class="mb-0">Multi-Agent Chatbot</h5>
          <small class="text-muted">{{ agentStatus }}</small>
        </div>
      </div>
    </b-navbar-brand>
    
    <b-navbar-nav class="ml-auto">
      <!-- User Info -->
      <b-nav-item v-if="user" class="d-flex align-items-center mr-3">
        <span class="text-light mr-2">
          👤 {{ user.nickname }}
        </span>
        <b-button
          @click="handleLogout"
          variant="outline-light"
          size="sm"
          class="logout-btn"
        >
          로그아웃
        </b-button>
      </b-nav-item>
      
      <!-- Connection Status -->
      <b-nav-item class="d-flex align-items-center">
        <div 
          class="status-indicator mr-2"
          :class="{
            'status-connected': connectionStatus === 'connected',
            'status-connecting': connectionStatus === 'connecting',
            'status-disconnected': connectionStatus === 'disconnected'
          }"
        />
        <span class="text-capitalize">{{ connectionStatus }}</span>
      </b-nav-item>
    </b-navbar-nav>
  </b-navbar>
</template>

<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';
import { User } from '@/types';

@Component
export default class ChatHeader extends Vue {
  @Prop({ type: String, default: 'disconnected' })
  connectionStatus!: string;

  @Prop({ type: String, default: '4개 전문 에이전트 대기 중' })
  agentStatus!: string;

  @Prop({ type: Object, default: null })
  user!: User | null;

  handleLogout(): void {
    this.$bvModal.msgBoxConfirm('로그아웃 하시겠습니까?', {
      title: '로그아웃 확인',
      size: 'sm',
      buttonSize: 'sm',
      okVariant: 'danger',
      okTitle: '로그아웃',
      cancelTitle: '취소',
      footerClass: 'p-2',
      hideHeaderClose: false,
      centered: true
    })
    .then((value: boolean) => {
      if (value) {
        this.$emit('logout');
      }
    })
    .catch(() => {
      // User cancelled
    });
  }
}
</script>

<style lang="scss" scoped>
.chat-header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  
  .status-indicator {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    
    &.status-connected {
      background-color: #28a745;
    }
    
    &.status-connecting {
      background-color: #ffc107;
    }
    
    &.status-disconnected {
      background-color: #dc3545;
    }
  }

  .logout-btn {
    font-size: 0.75rem;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    transition: all 0.2s ease;
    
    &:hover {
      background-color: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.3);
    }
  }
}
</style>