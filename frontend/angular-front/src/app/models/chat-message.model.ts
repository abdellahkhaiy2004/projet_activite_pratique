export interface ChatMessage {
  role: 'user' | 'bot';
  text: string;
  error?: boolean;
}
