import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '@/types';

const API_URL = 'http://localhost:8080/api';

interface HealthCheckResponse {
  status: string;
  timestamp: string;
}

interface UserResponse {
  id: string;
  nickname: string;
  createdAt: string;
}

interface UserLoginRequest {
  nickname: string;
}

interface ChatRoomCreateRequest {
  user_id: string;
  title?: string;
}

interface ChatRoomUpdateRequest {
  user_id: string;
  title: string;
}

interface ChatRoomDeleteRequest {
  user_id: string;
}

interface ChatRoomResponse {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  updated_at: string;
  last_message_at?: string;
}

// TODO: 서버에 구현되면 활성화할 인터페이스들
// interface ChatHistoryResponse {
//   messages: any[];
//   session_id: string;
// }

// interface SessionResponse {
//   message: string;
//   session_id: string;
// }

// interface ActiveSessionsResponse {
//   sessions: string[];
// }

// interface ServiceInfoResponse {
//   name: string;
//   version: string;
//   agents: string[];
// }

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });
    
    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor
    this.client.interceptors.request.use(
      (config: AxiosRequestConfig) => {
        // Add auth token if available
        // Note: We'll handle cookies properly in a real implementation
        return config;
      },
      (error: any) => {
        return Promise.reject(error);
      },
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response: AxiosResponse) => {
        return response;
      },
      (error: any) => {
        if (error.response && error.response.status === 401) {
          console.warn('Unauthorized access - consider redirecting to login');
        }
        return Promise.reject(error);
      },
    );
  }

  // Health check
  async healthCheck(): Promise<HealthCheckResponse> {
    try {
      const response = await this.client.get<HealthCheckResponse>('/health');
      return response.data;
    } catch (error: any) {
      throw new Error(`Health check failed: ${error.message}`);
    }
  }

  // User registration/login with nickname
  async loginWithNickname(nickname: string): Promise<UserResponse> {
    try {
      const request: UserLoginRequest = { nickname };
      const response = await this.client.post<UserResponse>('/users/login', request);
      return response.data;
    } catch (error: any) {
      throw new Error(`User login failed: ${error.message}`);
    }
  }

  // Multi-agent chat message (Spring Boot based)
  async sendMessage(message: string, sessionId: string, userId: string): Promise<ApiResponse> {
    try {
      const response = await this.client.post<ApiResponse>('/chatbot/chat', {
        message,
        sessionId,
        userId,
      });
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to send message: ${error.message}`);
    }
  }

  // TODO: 서버에 구현되면 활성화할 엔드포인트들
  
  // Get chat history - 서버에 구현 필요
  // async getChatHistory(sessionId: string): Promise<ChatHistoryResponse> {
  //   try {
  //     const response = await this.client.get<ChatHistoryResponse>(`/chatbot/history/${sessionId}`);
  //     return response.data;
  //   } catch (error: any) {
  //     throw new Error(`Failed to get chat history: ${error.message}`);
  //   }
  // }

  // Clear session - 서버에 구현 필요
  // async clearSession(sessionId: string): Promise<SessionResponse> {
  //   try {
  //     const response = await this.client.delete(`/chatbot/session/${sessionId}`);
  //     return response.data;
  //   } catch (error: any) {
  //     throw new Error(`Failed to clear session: ${error.message}`);
  //   }
  // }

  // Get active sessions - 서버에 구현 필요
  // async getActiveSessions(): Promise<ActiveSessionsResponse> {
  //   try {
  //     const response = await this.client.get<ActiveSessionsResponse>('/chatbot/sessions');
  //     return response.data;
  //   } catch (error: any) {
  //     throw new Error(`Failed to get active sessions: ${error.message}`);
  //   }
  // }

  // Get service information - 서버에 구현 필요
  // async getServiceInfo(): Promise<ServiceInfoResponse> {
  //   try {
  //     const response = await this.client.get<ServiceInfoResponse>('/chatbot/info');
  //     return response.data;
  //   } catch (error: any) {
  //     throw new Error(`Failed to get service info: ${error.message}`);
  //   }
  // }

  // Chat service health check - 기존 healthCheck() 사용
  // async chatHealthCheck(): Promise<HealthCheckResponse> {
  //   try {
  //     const response = await this.client.get<HealthCheckResponse>('/chatbot/health');
  //     return response.data;
  //   } catch (error: any) {
  //     throw new Error(`Chat health check failed: ${error.message}`);
  //   }
  // }

  // ChatRoom API methods
  async getChatRooms(userId: string): Promise<ChatRoomResponse[]> {
    try {
      const response = await this.client.get<ChatRoomResponse[]>('/chatrooms', {
        params: { user_id: userId }
      });
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to get chat rooms: ${error.message}`);
    }
  }

  async createChatRoom(userId: string, title?: string): Promise<ChatRoomResponse> {
    try {
      const request: ChatRoomCreateRequest = { user_id: userId, title };
      const response = await this.client.post<ChatRoomResponse>('/chatrooms', request);
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to create chat room: ${error.message}`);
    }
  }

  async getChatRoom(chatRoomId: string): Promise<ChatRoomResponse> {
    try {
      const response = await this.client.get<ChatRoomResponse>(`/chatrooms/${chatRoomId}`);
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to get chat room: ${error.message}`);
    }
  }

  async updateChatRoomTitle(chatRoomId: string, userId: string, title: string): Promise<ChatRoomResponse> {
    try {
      const request: ChatRoomUpdateRequest = { user_id: userId, title };
      const response = await this.client.put<ChatRoomResponse>(`/chatrooms/${chatRoomId}`, request);
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to update chat room: ${error.message}`);
    }
  }

  async deleteChatRoom(chatRoomId: string, userId: string): Promise<void> {
    try {
      const request: ChatRoomDeleteRequest = { user_id: userId };
      await this.client.delete(`/chatrooms/${chatRoomId}`, { data: request });
    } catch (error: any) {
      throw new Error(`Failed to delete chat room: ${error.message}`);
    }
  }

  // Streaming chat message using Server-Sent Events
  async sendStreamingMessage(
    message: string, 
    sessionId: string,
    userId: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    let eventSource: EventSource | null = null;
    
    try {
      // URL parameters를 더 안전하게 구성
      const params = new URLSearchParams({
        message,
        sessionId,
        userId
      });
      const url = `${API_URL}/chatbot/chat/stream?${params.toString()}`;
      
      eventSource = new EventSource(url);
      
      eventSource.onmessage = (event) => {
        try {
          // 기본 메시지 이벤트 처리
          const data = event.data.trim();
          if (data === '[DONE]') {
            eventSource?.close();
            onComplete();
            return;
          }
          
          const streamingResponse = JSON.parse(data);
          
          if (streamingResponse.chunk) {
            onChunk(streamingResponse.chunk);
          }
          
          if (streamingResponse.isLast || streamingResponse.done) {
            eventSource?.close();
            onComplete();
          }
        } catch (parseError) {
          console.warn('Failed to parse streaming data:', parseError);
          // JSON 파싱 실패시 raw text로 처리
          const data = event.data.trim();
          if (data && data !== '[DONE]') {
            onChunk(data);
          }
        }
      };
      
      eventSource.onerror = (event) => {
        console.error('EventSource error:', event);
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
        onError(new Error('스트리밍 연결 에러가 발생했습니다.'));
      };
      
      eventSource.onopen = () => {
        console.log('EventSource connection opened');
      };
      
    } catch (error: any) {
      if (eventSource) {
        eventSource.close();
      }
      onError(new Error(`스트리밍 요청 실패: ${error.message}`));
    }
  }
}

export default new ApiService();