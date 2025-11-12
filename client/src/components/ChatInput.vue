<template>
  <div class="chat-input-container">
    <b-input-group class="chat-input">
      <b-form-textarea
        ref="messageInput"
        v-model="message"
        @keydown.enter.prevent="handleEnterKey"
        @keydown="handleKeydown"
        :disabled="disabled"
        :placeholder="disabled ? 'AI가 응답 중입니다...' : '메시지를 입력하세요... (Shift+Enter: 줄바꿈, Enter: 전송)'"
        :rows="rows"
        :max-rows="5"
        auto-grow
        trim
        class="message-textarea"
      />
      <b-input-group-append>
        <b-button
          @click="sendMessage"
          :disabled="disabled || !message.trim()"
          variant="primary"
          class="send-button"
        >
          <b-spinner v-if="disabled" small />
          <SendIcon v-else width="16" height="16" />
        </b-button>
      </b-input-group-append>
    </b-input-group>
    
    <!-- Character Count Warning -->
    <div 
      v-if="message.length > 5000"
      class="character-count mt-1 text-right"
      :class="{
        'text-warning': message.length > 5000 && message.length <= 8000,
        'text-danger': message.length > 8000
      }"
    >
      <small>{{ message.length }}/10000</small>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import SendIcon from 'vue-material-design-icons/Send.vue';

@Component({
  components: {
    SendIcon,
  },
})
export default class ChatInput extends Vue {
  @Prop({ type: Boolean, default: false })
  disabled!: boolean;

  message: string = '';
  rows: number = 1;

  @Watch('message')
  onMessageChange(): void {
    this.adjustRows();
  }

  sendMessage(): void {
    const trimmedMessage = this.message.trim();
    if (!trimmedMessage || this.disabled) return;

    if (trimmedMessage.length > 10000) {
      this.$bvToast.toast('메시지가 너무 깁니다. 10000자 이하로 입력해주세요.', {
        title: '입력 오류',
        variant: 'warning',
        solid: true,
      });
      return;
    }

    this.$emit('send-message', trimmedMessage);
    this.message = '';
    this.rows = 1;
    this.focusInput();
  }

  handleEnterKey(event: KeyboardEvent): void {
    if (!event.shiftKey) {
      this.sendMessage();
    }
  }

  handleKeydown(event: KeyboardEvent): void {
    // Allow Shift+Enter for new lines
    if (event.key === 'Enter' && event.shiftKey) {
      // Let the default behavior handle this
      return;
    }
  }

  adjustRows(): void {
    const lineHeight = 20;
    const padding = 12;
    
    this.$nextTick(() => {
      if (this.$refs.messageInput && (this.$refs.messageInput as any).$el) {
        const textarea = (this.$refs.messageInput as any).$el;
        textarea.style.height = 'auto';
        const scrollHeight = textarea.scrollHeight;
        const newRows = Math.min(5, Math.max(1, Math.floor(scrollHeight / lineHeight)));
        this.rows = newRows;
      }
    });
  }

  focusInput(): void {
    this.$nextTick(() => {
      if (this.$refs.messageInput && (this.$refs.messageInput as any).focus) {
        (this.$refs.messageInput as any).focus();
      }
    });
  }

  mounted(): void {
    this.focusInput();
  }
}
</script>

<style lang="scss" scoped>
.chat-input {
  .message-textarea {
    border-top-right-radius: 0 !important;
    border-bottom-right-radius: 0 !important;
    resize: none;
    
    &:focus {
      box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
    }
  }
  
  .send-button {
    border-top-left-radius: 0 !important;
    border-bottom-left-radius: 0 !important;
    padding: 0.5rem 0.75rem;
    
    &:disabled {
      opacity: 0.5;
    }
  }
}

.character-count {
  font-size: 0.75rem;
}
</style>