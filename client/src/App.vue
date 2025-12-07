<template>
  <div id="app">
    <chat-header 
      :connection-status="connectionStatus"
      :agent-status="agentStatus"
      :user="user"
    />
    <router-view id="content" :key="$route.path" />
    <chat-footer />
  </div>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';
import { mapState } from 'vuex';
import ChatHeader from './components/ChatHeader.vue';
import ChatFooter from './components/ChatFooter.vue';
import apiService from './services/ApiService';

@Component({
  components: {
    ChatHeader,
    ChatFooter,
  },
  computed: {
    ...mapState(['user']),
  },
})
export default class App extends Vue {
  connectionStatus: string = 'disconnected';
  agentStatus: string = '4개 전문 에이전트 대기 중';
  sessionId: string = this.generateSessionId();

  get currentRoute(): string {
    return this.$route.path;
  }

  watch: any = {
    currentRoute(newRoute: string, oldRoute: string) {
      if (newRoute !== oldRoute) {
        this.updateRouteContext();
      }
    },
  };

  generateSessionId(): string {
    return 'session_' + Math.random().toString(36).substr(2, 9);
  }

  async checkConnection(): Promise<void> {
    try {
      this.connectionStatus = 'connecting';
      await apiService.healthCheck();
      this.connectionStatus = 'connected';
      this.agentStatus = '🎯 Supervisor, 📚 RAG,  🔍 Search, 💬 General';
    } catch (error) {
      console.error('Connection failed:', error);
      this.connectionStatus = 'disconnected';
      this.agentStatus = '서버 연결 실패';
    }
  }

  updateRouteContext(): void {
    // Route change handling logic
  }

  async mounted(): Promise<void> {
    // Load user from localStorage
    this.$store.commit('loadUserFromStorage');
    
    await this.checkConnection();
  }
}
</script>

<style lang="scss">
// Variables are now globally imported via vite.config.js

#app {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  font-family: 'Arial', sans-serif;
}

#content {
  flex: 1;
}
</style>