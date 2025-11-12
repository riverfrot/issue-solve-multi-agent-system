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

@Component
export default class ChatHeader extends Vue {
  @Prop({ type: String, default: 'disconnected' })
  connectionStatus!: string;

  @Prop({ type: String, default: '4개 전문 에이전트 대기 중' })
  agentStatus!: string;
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
}
</style>