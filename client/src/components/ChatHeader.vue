<template>
  <b-navbar class="chat-header" variant="dark">
    <b-navbar-brand>
      <div class="d-flex align-items-center">
        <div>
          <h5 class="mb-0 text-white">Issue-solve chatbot</h5>
          <small class="text-muted">
            <a 
              href="https://github.com/riverfrot/issue-solve-multi-agent-system" 
              target="_blank" 
              class="github-link"
            >
              GitHub Repository
            </a>
          </small>
        </div>
      </div>
    </b-navbar-brand>
    
    <b-navbar-nav class="ml-auto">
      <!-- User Info -->
      <b-nav-item v-if="user" class="d-flex align-items-center mr-3">
        <span class="text-light mr-2">
          {{ user.nickname }}
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

  @Prop({ type: String, default: '' })
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

  .github-link {
    color: #8fc7ff;
    text-decoration: none;
    transition: color 0.2s ease;
    
    &:hover {
      color: #ffffff;
      text-decoration: underline;
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