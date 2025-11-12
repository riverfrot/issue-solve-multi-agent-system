import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '@/types';

const API_URL = import.meta.env.VITE_API_URL || '';

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
      const response = await this.client.get<HealthCheckResponse>('/api/health');
      return response.data;
    } catch (error: any) {
      throw new Error(`Health check failed: ${error.message}`);
    }
  }

  // Multi-agent chat message (LangGraph based)
  async sendMessage(message: string, sessionId: string): Promise<ApiResponse> {
    try {
      const response = await this.client.post<ApiResponse>('/api/chat/', {
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
}

export default new ApiService();