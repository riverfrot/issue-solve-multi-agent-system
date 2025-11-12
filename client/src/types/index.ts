export interface Message {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  agent_type: string;
  metadata?: MessageMetadata;
  thinking_process?: any;
  error?: boolean;
  welcome?: boolean;
}

export interface MessageMetadata {
  processing_time?: number;
  agent_used?: string;
  [key: string]: any;
}

export interface ApiResponse {
  message: string;
  agent_used: string;
  metadata: MessageMetadata;
  thinking_process?: any;
}

export interface QuickAction {
  emoji: string;
  label: string;
  text: string;
}

export interface AgentStatus {
  supervisor: string;
  rag: string;
  code: string;
  search: string;
  general: string;
  [key: string]: string;
}

export interface ConnectionStatus {
  status: 'connected' | 'connecting' | 'disconnected';
}

export interface AppState {
  messages: Message[];
  isLoading: boolean;
  connectionStatus: 'connected' | 'connecting' | 'disconnected';
  currentAgent: string;
  sessionId: string | null;
  agentStatus: string;
}

export interface NavigationItem {
  text: string;
  link?: string;
  external?: string;
  value?: string;
}