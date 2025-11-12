<template>
  <b-navbar class="chat-footer">
    <div class="w-100 text-center">
      <small class="text-muted">
        Vue.js + FastAPI + Multi-Agent System | v1.0.0
      </small>
    </div>
  </b-navbar>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';

@Component
export default class ChatFooter extends Vue {}
</script>

<style lang="scss" scoped>
.chat-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding: 0.5rem 1rem;
  
  small {
    font-size: 0.75rem;
  }
}
</style>