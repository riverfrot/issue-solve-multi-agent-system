import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '@/types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000/api';

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

  // Streaming chat message using Server-Sent Events
  async sendStreamingMessage(
    message: string, 
    sessionId: string,
    userId: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    try {
      const url = `${this.client.defaults.baseURL}/chatbot/chat/stream?message=${encodeURIComponent(message)}&sessionId=${encodeURIComponent(sessionId)}&userId=${encodeURIComponent(userId)}`;
      
      const eventSource = new EventSource(url);
      
      eventSource.onmessage = (event) => {
        try {
          const streamingResponse = JSON.parse(event.data);
          if (streamingResponse.chunk) {
            onChunk(streamingResponse.chunk + ' '); // 단어 간격 추가
          }
          
          if (streamingResponse.isLast) {
            eventSource.close();
            onComplete();
          }
        } catch (parseError) {
          console.warn('Failed to parse streaming data:', parseError);
          // Fallback to plain text
          const data = event.data.trim();
          if (data) {
            onChunk(data);
          }
        }
      };
      
      // Handle specific event types
      eventSource.addEventListener('chunk', (event: any) => {
        try {
          const streamingResponse = JSON.parse(event.data);
          if (streamingResponse.chunk) {
            onChunk(streamingResponse.chunk + ' '); // 단어 간격 추가
          }
          
          if (streamingResponse.isLast) {
            eventSource.close();
            onComplete();
          }
        } catch (parseError) {
          console.warn('Failed to parse chunk event:', parseError);
        }
      });
      
      eventSource.onerror = (event) => {
        console.error('EventSource error:', event);
        eventSource.close();
        onError(new Error('스트리밍 연결 에러가 발생했습니다.'));
      };
      
      eventSource.addEventListener('complete', () => {
        eventSource.close();
        onComplete();
      });
      
    } catch (error: any) {
      onError(new Error(`스트리밍 요청 실패: ${error.message}`));
    }
  }
}

export default new ApiService();