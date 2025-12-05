import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '@/types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

interface HealthCheckResponse {
  status: string;
  timestamp: string;
}

interface ChatHistoryResponse {
  messages: any[];
  session_id: string;
}

interface SessionResponse {
  message: string;
  session_id: string;
}

interface ActiveSessionsResponse {
  sessions: string[];
}

interface ServiceInfoResponse {
  name: string;
  version: string;
  agents: string[];
}

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
      const response = await this.client.get<HealthCheckResponse>('/chatbot/health');
      return response.data;
    } catch (error: any) {
      throw new Error(`Health check failed: ${error.message}`);
    }
  }

  // Multi-agent chat message (LangGraph based)
  async sendMessage(message: string, sessionId: string): Promise<ApiResponse> {
    try {
      const response = await this.client.post<ApiResponse>('/chat/', {
        message,
        session_id: sessionId,
      });
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to send message: ${error.message}`);
    }
  }

  // Get chat history
  async getChatHistory(sessionId: string): Promise<ChatHistoryResponse> {
    try {
      const response = await this.client.get<ChatHistoryResponse>(`/api/chat/history/${sessionId}`);
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to get chat history: ${error.message}`);
    }
  }

  // Clear session
  async clearSession(sessionId: string): Promise<SessionResponse> {
    try {
      const response = await this.client.delete(`/api/chat/session/${sessionId}`);
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to clear session: ${error.message}`);
    }
  }

  // Get active sessions
  async getActiveSessions(): Promise<ActiveSessionsResponse> {
    try {
      const response = await this.client.get<ActiveSessionsResponse>('/api/chat/sessions');
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to get active sessions: ${error.message}`);
    }
  }

  // Get service information
  async getServiceInfo(): Promise<ServiceInfoResponse> {
    try {
      const response = await this.client.get<ServiceInfoResponse>('/api/chat/info');
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to get service info: ${error.message}`);
    }
  }

  // Chat service health check
  async chatHealthCheck(): Promise<HealthCheckResponse> {
    try {
      const response = await this.client.get<HealthCheckResponse>('/api/chat/health');
      return response.data;
    } catch (error: any) {
      throw new Error(`Chat health check failed: ${error.message}`);
    }
  }

  // Test workflow
  async testWorkflow(testQuery: string = '안녕하세요'): Promise<ApiResponse> {
    try {
      const response = await this.client.post<ApiResponse>('/api/chat/workflow/test', null, {
        params: { test_query: testQuery },
      });
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to test workflow: ${error.message}`);
    }
  }

  // Streaming chat message using Server-Sent Events
  async sendStreamingMessage(
    message: string, 
    sessionId: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    try {
      const url = `${this.client.defaults.baseURL}/chatbot/chat/stream?message=${encodeURIComponent(message)}&sessionId=${encodeURIComponent(sessionId)}`;
      
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