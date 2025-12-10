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
        :disabled="!isNicknameValid || isLoading"
        @click="handleSubmit"
        class="px-4"
      >
        <b-spinner v-if="isLoading" small class="mr-1" />
        <b-icon v-else icon="check-circle" class="mr-1" />
        {{ isLoading ? '로그인 중...' : '시작하기' }}
      </b-button>
    </template>
  </b-modal>
</template>

<script>
import apiService from '@/services/ApiService';

export default {
  name: 'UserNicknameModal',
  data() {
    return {
      nickname: '',
      isLoading: false,
    };
  },
  computed: {
    nicknameState() {
      if (this.nickname.length === 0) return null;
      return this.isNicknameValid;
    },
    isNicknameValid() {
      return this.nickname.trim().length > 0 && this.nickname.trim().length <= 50;
    },
    nicknameErrorMessage() {
      if (this.nickname.trim().length === 0) {
        return '닉네임은 비어있을 수 없습니다.';
      }
      if (this.nickname.trim().length > 50) {
        return '닉네임은 50자를 초과할 수 없습니다.';
      }
      return '';
    },
  },
  methods: {
    show() {
      this.$refs.nicknameModal.show();
    },
    hide() {
      this.$refs.nicknameModal.hide();
    },
    focusInput() {
      this.$nextTick(() => {
        if (this.$refs.nicknameInput) {
          this.$refs.nicknameInput.focus();
        }
      });
    },
    async handleSubmit() {
      if (!this.isNicknameValid || this.isLoading) {
        return;
      }

      const trimmedNickname = this.nickname.trim();
      
      try {
        this.isLoading = true;
        
        // Call server API for login with nickname
        const userResponse = await apiService.loginWithNickname(trimmedNickname);
        
        // Emit event to parent component with server response
        this.$emit('nickname-submitted', {
          nickname: userResponse.nickname,
          userId: userResponse.id
        });

        this.hide();
        
      } catch (error) {
        console.error('Login failed:', error);
        this.$toasted.error('로그인에 실패했습니다. 다시 시도해주세요.');
      } finally {
        this.isLoading = false;
      }
    },
  },
};
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