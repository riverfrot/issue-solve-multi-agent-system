-- 테스트 대화 세션 생성
INSERT INTO conversations (session_id, user_id, started_at) VALUES 
('test-session-001', 'user001', '2024-01-15 10:00:00'),
('test-session-002', 'user001', '2024-01-15 14:30:00'),
('test-session-003', 'user002', '2024-01-16 09:15:00');

-- 테스트 채팅 메시지 생성
INSERT INTO chat_messages (id, session_id, role, content, agent_type, timestamp) VALUES 
-- Session 1 대화
('msg-001', 'test-session-001', 'user', '안녕하세요, 코딩 도움이 필요해요', null, '2024-01-15 10:01:00'),
('msg-002', 'test-session-001', 'assistant', '안녕하세요! 어떤 코딩 문제로 도와드릴까요?', 'GENERAL', '2024-01-15 10:01:30'),
('msg-003', 'test-session-001', 'user', 'Python으로 간단한 웹크롤러를 만들고 싶어요', null, '2024-01-15 10:02:00'),
('msg-004', 'test-session-001', 'assistant', 'Python 웹크롤링 코드를 생성해드리겠습니다.', 'CODE', '2024-01-15 10:02:30'),

-- Session 2 대화  
('msg-005', 'test-session-002', 'user', '최신 뉴스 검색해줄 수 있나요?', null, '2024-01-15 14:31:00'),
('msg-006', 'test-session-002', 'assistant', '네, 최신 뉴스를 검색해드리겠습니다.', 'SEARCH', '2024-01-15 14:31:30'),

-- Session 3 대화
('msg-007', 'test-session-003', 'user', '회사 문서에서 정보를 찾아주세요', null, '2024-01-16 09:16:00'),
('msg-008', 'test-session-003', 'assistant', '문서를 검색해보겠습니다.', 'RAG', '2024-01-16 09:16:30');

-- 메타데이터 테스트 데이터
INSERT INTO chat_message_metadata (message_id, metadata_key, metadata_value) VALUES 
('msg-004', 'language', 'python'),
('msg-004', 'complexity', 'beginner'),
('msg-006', 'search_query', '최신 뉴스'),
('msg-008', 'document_type', 'company_docs');