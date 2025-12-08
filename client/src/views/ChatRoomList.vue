<template>
  <div class="chat-room-container" :class="{ 'chat-selected': selectedChatRoomId }">
    <!-- Left Sidebar -->
    <div class="sidebar">
      <!-- Header with Menu and Search -->
      <div class="sidebar-header">
        <div class="header-left">
          <b-button variant="link" class="menu-btn p-0">
            <b-icon icon="list" font-scale="1.2" />
          </b-button>
        </div>
        <div class="header-right">
          <b-button variant="link" class="search-btn p-0">
            <b-icon icon="search" font-scale="1.2" />
          </b-button>
        </div>
      </div>

      <!-- New Chat Button -->
      <div class="new-chat-section">
        <b-button 
          @click="createNewChatRoom" 
          variant="outline-secondary"
          class="new-chat-btn w-100"
          :disabled="isCreatingChat"
        >
          <b-icon icon="plus-circle" class="mr-2" />
          새 채팅
        </b-button>
      </div>

      <!-- Chat History Title -->
      <div class="history-title">
        <small class="text-muted">내 활동</small>
      </div>

      <!-- Chat Rooms List -->
      <div class="chatrooms-list">
        <div v-if="isLoading" class="loading-container text-center py-3">
          <b-spinner variant="primary" class="mr-2" />
          <span class="text-muted">채팅방 로딩 중...</span>
        </div>

        <div v-else-if="chatRooms.length === 0" class="empty-state text-center py-4">
          <p class="text-muted mb-0">아직 채팅방이 없습니다</p>
          <small class="text-muted">새 채팅을 시작해보세요!</small>
        </div>

        <div v-else class="chatroom-items">
          <div 
            v-for="chatroom in chatRooms"
            :key="chatroom.id"
            @click="selectChatRoom(chatroom)"
            :class="['chatroom-item', { active: selectedChatRoomId === chatroom.id }]"
          >
            <div class="chatroom-content">
              <div class="chatroom-title">{{ chatroom.title }}</div>
              <div class="chatroom-time">{{ formatTime(chatroom.updated_at) }}</div>
            </div>
            <div class="chatroom-actions">
              <b-dropdown 
                right 
                no-caret 
                variant="link" 
                size="sm"
                @click.stop
              >
                <template #button-content>
                  <b-icon icon="three-dots-vertical" font-scale="0.8" />
                </template>
                <b-dropdown-item @click="editChatRoomTitle(chatroom)">
                  <b-icon icon="pencil" class="mr-2" />
                  제목 수정
                </b-dropdown-item>
                <b-dropdown-item @click="deleteChatRoom(chatroom)" variant="danger">
                  <b-icon icon="trash" class="mr-2" />
                  삭제
                </b-dropdown-item>
              </b-dropdown>
            </div>
          </div>
        </div>
      </div>

      <!-- User Info Section -->
      <div class="user-section">
        <div class="user-info">
          <div class="user-avatar">
            {{ getUserInitial() }}
          </div>
          <div class="user-details">
            <div class="user-name">{{ user?.nickname || '사용자' }}</div>
            <small class="text-muted">온라인</small>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content Area -->
    <div class="main-content">
      <div v-if="!isUserLoggedIn" class="welcome-screen">
        <div class="welcome-container text-center">
          <h2 class="welcome-title mb-4">로그인이 필요합니다</h2>
          <p class="welcome-subtitle mb-4">
            채팅을 시작하려면 먼저 로그인해주세요.
          </p>
          <b-button 
            @click="showLoginModal" 
            variant="primary"
            size="lg"
          >
            <b-icon icon="person-plus" class="mr-2" />
            로그인하기
          </b-button>
        </div>
      </div>
      
      <div v-else-if="!selectedChatRoomId" class="welcome-screen">
        <div class="welcome-container text-center">
          <h2 class="welcome-title mb-4">안녕하세요!</h2>
          <p class="welcome-subtitle mb-4">
            AI와 대화를 시작하거나 기존 채팅방을 선택해주세요.
          </p>
          <b-button 
            @click="createNewChatRoom" 
            variant="primary"
            size="lg"
            :disabled="isCreatingChat"
          >
            <b-icon icon="chat-dots" class="mr-2" />
            새 채팅 시작
          </b-button>
        </div>
      </div>
      
      <div v-else class="chat-content">
        <chat-room :session-id="selectedChatRoomId" />
      </div>
    </div>

    <!-- Edit Title Modal -->
    <b-modal 
      ref="editTitleModal" 
      title="채팅방 제목 수정"
      @ok="updateChatRoomTitle"
      ok-title="수정"
      cancel-title="취소"
    >
      <b-form-group label="새 제목">
        <b-form-input 
          v-model="newTitle" 
          placeholder="채팅방 제목을 입력하세요"
          maxlength="100"
        />
      </b-form-group>
    </b-modal>

    <!-- User Nickname Modal -->
    <user-nickname-modal 
      ref="nicknameModal" 
      @nickname-submitted="handleNicknameSubmitted" 
    />
  </div>
</template>

<script>
import { mapState } from 'vuex';
import ChatRoom from '@/views/ChatRoom.vue';
import UserNicknameModal from '@/components/UserNicknameModal.vue';
import apiService from '@/services/ApiService';

export default {
  name: 'ChatRoomList',
  components: {
    ChatRoom,
    UserNicknameModal,
  },
  data() {
    return {
      chatRooms: [],
      selectedChatRoomId: null,
      isLoading: false,
      isCreatingChat: false,
      editingChatRoom: null,
      newTitle: '',
    };
  },
  computed: {
    ...mapState(['user', 'isUserLoggedIn']),
  },
  async mounted() {
    if (this.isUserLoggedIn) {
      await this.loadChatRooms();
    }
  },
  watch: {
    isUserLoggedIn(newValue) {
      if (newValue) {
        this.loadChatRooms();
      } else {
        this.chatRooms = [];
        this.selectedChatRoomId = null;
      }
    },
  },
  methods: {
    async loadChatRooms() {
      if (!this.user?.id) return;

      try {
        this.isLoading = true;
        this.chatRooms = await apiService.getChatRooms(this.user.id);
      } catch (error) {
        console.error('Failed to load chat rooms:', error);
        this.$toasted.error('채팅방 목록을 불러오는데 실패했습니다.');
      } finally {
        this.isLoading = false;
      }
    },

    async createNewChatRoom() {
      if (!this.user?.id || this.isCreatingChat) return;

      try {
        this.isCreatingChat = true;
        const newChatRoom = await apiService.createChatRoom(this.user.id);
        
        // Add to list and select
        this.chatRooms.unshift(newChatRoom);
        this.selectedChatRoomId = newChatRoom.id;
        
        this.$toasted.success('새 채팅방이 생성되었습니다.');
      } catch (error) {
        console.error('Failed to create chat room:', error);
        this.$toasted.error('채팅방 생성에 실패했습니다.');
      } finally {
        this.isCreatingChat = false;
      }
    },

    selectChatRoom(chatroom) {
      this.selectedChatRoomId = chatroom.id;
    },

    editChatRoomTitle(chatroom) {
      this.editingChatRoom = chatroom;
      this.newTitle = chatroom.title;
      this.$refs.editTitleModal.show();
    },

    async updateChatRoomTitle() {
      if (!this.editingChatRoom || !this.newTitle.trim()) return;

      try {
        const updatedChatRoom = await apiService.updateChatRoomTitle(
          this.editingChatRoom.id,
          this.user.id,
          this.newTitle.trim()
        );

        // Update in list
        const index = this.chatRooms.findIndex(room => room.id === this.editingChatRoom.id);
        if (index !== -1) {
          this.chatRooms.splice(index, 1, updatedChatRoom);
        }

        this.$toasted.success('채팅방 제목이 수정되었습니다.');
      } catch (error) {
        console.error('Failed to update chat room title:', error);
        this.$toasted.error('제목 수정에 실패했습니다.');
      }

      this.editingChatRoom = null;
      this.newTitle = '';
    },

    async deleteChatRoom(chatroom) {
      const confirmed = await this.$swal({
        title: '채팅방 삭제',
        text: `"${chatroom.title}" 채팅방을 삭제하시겠습니까?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '삭제',
        cancelButtonText: '취소',
      }).then((result) => result.isConfirmed);

      if (!confirmed) return;

      try {
        await apiService.deleteChatRoom(chatroom.id, this.user.id);
        
        // Remove from list
        this.chatRooms = this.chatRooms.filter(room => room.id !== chatroom.id);
        
        // Clear selection if deleted chatroom was selected
        if (this.selectedChatRoomId === chatroom.id) {
          this.selectedChatRoomId = null;
        }

        this.$toasted.success('채팅방이 삭제되었습니다.');
      } catch (error) {
        console.error('Failed to delete chat room:', error);
        this.$toasted.error('채팅방 삭제에 실패했습니다.');
      }
    },

    formatTime(timestamp) {
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));

      if (days === 0) {
        return date.toLocaleTimeString('ko-KR', {
          hour12: false,
          hour: '2-digit',
          minute: '2-digit',
        });
      } else if (days === 1) {
        return '어제';
      } else if (days < 7) {
        return `${days}일 전`;
      } else {
        return date.toLocaleDateString('ko-KR', {
          month: 'short',
          day: 'numeric',
        });
      }
    },

    getUserInitial() {
      return this.user?.nickname?.charAt(0).toUpperCase() || 'U';
    },

    showLoginModal() {
      // Show nickname modal for user registration
      this.$refs.nicknameModal.show();
    },

    handleNicknameSubmitted(userData) {
      // Store user data in Vuex store
      this.$store.commit('updateUser', {
        id: userData.userId,
        nickname: userData.nickname,
      });
      
      this.$toasted.success(`환영합니다, ${userData.nickname}님!`);
    },
  },
};
</script>

<style lang="scss" scoped>
.chat-room-container {
  display: flex;
  height: calc(100vh - 120px);
  background-color: #f8f9fa;
}

.sidebar {
  width: 300px;
  background-color: #ffffff;
  border-right: 1px solid #dee2e6;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid #dee2e6;
  background-color: #f8f9fa;

  .header-left, .header-right {
    width: 40px;
  }

  .menu-btn, .search-btn {
    color: #6c757d;
    
    &:hover {
      color: #495057;
    }
  }

  h5 {
    color: #495057;
    font-weight: 500;
  }
}

.new-chat-section {
  padding: 1rem;
  border-bottom: 1px solid #dee2e6;

  .new-chat-btn {
    border: 1px solid #dee2e6;
    border-radius: 8px;
    transition: all 0.2s ease;

    &:hover {
      background-color: #f8f9fa;
      border-color: #ced4da;
    }

    &:disabled {
      opacity: 0.6;
    }
  }
}

.history-title {
  padding: 1rem 1rem 0.5rem;
  font-weight: 500;
}

.chatrooms-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 0.5rem;
}

.loading-container {
  color: #6c757d;
}

.empty-state {
  margin-top: 2rem;
}

.chatroom-item {
  display: flex;
  align-items: center;
  padding: 0.75rem;
  margin: 0.25rem 0;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background-color: #f8f9fa;
  }

  &.active {
    background-color: #e3f2fd;
    border-left: 3px solid #2196f3;
  }

  .chatroom-content {
    flex: 1;
    min-width: 0;
  }

  .chatroom-title {
    font-size: 0.9rem;
    font-weight: 500;
    color: #495057;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 0.25rem;
  }

  .chatroom-time {
    font-size: 0.75rem;
    color: #6c757d;
  }

  .chatroom-actions {
    margin-left: 0.5rem;
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover .chatroom-actions {
    opacity: 1;
  }
}

.user-section {
  padding: 1rem;
  border-top: 1px solid #dee2e6;
  background-color: #f8f9fa;

  .user-info {
    display: flex;
    align-items: center;
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: #007bff;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 0.8rem;
    margin-right: 0.75rem;
  }

  .user-name {
    font-size: 0.9rem;
    font-weight: 500;
    color: #495057;
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.welcome-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;

  .welcome-container {
    max-width: 400px;
  }

  .welcome-title {
    color: #495057;
    font-weight: 300;
  }

  .welcome-subtitle {
    color: #6c757d;
    font-size: 1.1rem;
  }
}

.chat-content {
  flex: 1;
  background-color: #ffffff;
}

// Responsive design
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    position: absolute;
    z-index: 10;
    height: 100%;
  }

  .main-content {
    display: none;
  }

  .chat-room-container.chat-selected {
    .sidebar {
      display: none;
    }

    .main-content {
      display: flex;
    }
  }
}
</style>