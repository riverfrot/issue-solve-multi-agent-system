<template>
  <b-modal
    id="nickname-modal"
    ref="nicknameModal"
    title="사용자 정보 입력"
    :no-close-on-backdrop="true"
    :no-close-on-esc="true"
    :hide-header-close="true"
    size="md"
    centered
    @shown="focusInput"
  >
    <div class="text-center mb-4">
      <h5>👋 환영합니다!</h5>
      <p class="text-muted">채팅을 시작하기 전에 닉네임을 입력해주세요.</p>
    </div>

    <b-form @submit.prevent="handleSubmit">
      <b-form-group
        label="닉네임"
        label-for="nickname-input"
        description="채팅에서 사용될 닉네임을 입력해주세요 (최대 50자)"
      >
        <b-form-input
          id="nickname-input"
          ref="nicknameInput"
          v-model="nickname"
          type="text"
          placeholder="닉네임을 입력해주세요"
          :state="nicknameState"
          trim
          maxlength="50"
          @keyup.enter="handleSubmit"
        />
        <b-form-invalid-feedback>
          {{ nicknameErrorMessage }}
        </b-form-invalid-feedback>
      </b-form-group>
    </b-form>

    <template #modal-footer>
      <b-button
        variant="primary"
        :disabled="!isNicknameValid"
        @click="handleSubmit"
        class="px-4"
      >
        <b-icon icon="check-circle" class="mr-1" />
        시작하기
      </b-button>
    </template>
  </b-modal>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';

@Component
export default class UserNicknameModal extends Vue {
  nickname = '';

  get nicknameState(): boolean | null {
    if (this.nickname.length === 0) return null;
    return this.isNicknameValid;
  }

  get isNicknameValid(): boolean {
    return this.nickname.trim().length > 0 && this.nickname.trim().length <= 50;
  }

  get nicknameErrorMessage(): string {
    if (this.nickname.trim().length === 0) {
      return '닉네임은 비어있을 수 없습니다.';
    }
    if (this.nickname.trim().length > 50) {
      return '닉네임은 50자를 초과할 수 없습니다.';
    }
    return '';
  }

  show(): void {
    this.$refs.nicknameModal.show();
  }

  hide(): void {
    this.$refs.nicknameModal.hide();
  }

  focusInput(): void {
    this.$nextTick(() => {
      if (this.$refs.nicknameInput) {
        this.$refs.nicknameInput.focus();
      }
    });
  }

  handleSubmit(): void {
    if (!this.isNicknameValid) {
      return;
    }

    const trimmedNickname = this.nickname.trim();
    
    // Emit event to parent component
    this.$emit('nickname-submitted', {
      nickname: trimmedNickname,
      userId: this.generateUserId(trimmedNickname)
    });

    this.hide();
  }

  private generateUserId(nickname: string): string {
    const timestamp = Date.now();
    const randomSuffix = Math.random().toString(36).substr(2, 6);
    return `user_${timestamp}_${randomSuffix}`;
  }
}
</script>

<style scoped>
.modal-header {
  border-bottom: 1px solid #dee2e6;
}

.modal-footer {
  border-top: 1px solid #dee2e6;
  justify-content: center;
}

#nickname-input {
  font-size: 1.1rem;
  padding: 0.75rem;
}

.modal-body {
  padding: 2rem;
}
</style>