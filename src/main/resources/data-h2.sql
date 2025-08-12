-- 회사 데이터
INSERT INTO company (company_name, created_at) 
VALUES ('비상(Visang) 주식회사', CURRENT_TIMESTAMP());

-- 관리자 데이터
-- password: @Pwd2025
INSERT INTO admin (company_id, name, email, password, phone_number, address, role, created_at, email_verified) 
VALUES (1, '관리자', 'admin@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-1234-5678', '서울시 강남구 테헤란로 123', 'ADMIN', CURRENT_TIMESTAMP(), TRUE);

-- 상담원 데이터
-- password: @Pwd2025
INSERT INTO agent (company_id, name, email, password, phone_number, address, role, state, created_at, profile_image_url, email_verified) 
VALUES (1, '상담원', 'agent@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-2345-6789', '서울시 서초구 강남대로 456', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP(), NULL, TRUE);

-- 고객 데이터
-- password: @Pwd2025
INSERT INTO client (ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified) 
VALUES ('910101-1234567', '고객', 'user@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-3456-7890', '서울시 송파구 올림픽로 789', 'USER', CURRENT_TIMESTAMP(), NULL, TRUE);

-- 계약 더미 데이터 추가
-- 진행중인 계약
INSERT INTO contract (client_id, agent_id, contract_date, start_date, end_date, consultation_type, status, created_at, total_price)
VALUES (1, 1, CURRENT_DATE(), CURRENT_DATE(), DATEADD('DAY', 30, CURRENT_DATE()), 'VOICE', 'IN_PROGRESS', CURRENT_TIMESTAMP(), 100000);

-- 오늘 예정된 계약
INSERT INTO contract (client_id, agent_id, contract_date, start_date, end_date, consultation_type, status, created_at, total_price)
VALUES (1, 1, CURRENT_DATE(), CURRENT_DATE(), DATEADD('DAY', 60, CURRENT_DATE()), 'VIDEO', 'PENDING', CURRENT_TIMESTAMP(), 150000);

-- 완료된 계약
INSERT INTO contract (client_id, agent_id, contract_date, start_date, end_date, consultation_type, status, created_at, total_price)
VALUES (1, 1, DATEADD('DAY', -30, CURRENT_DATE()), DATEADD('DAY', -30, CURRENT_DATE()), CURRENT_DATE(), 'VOICE', 'COMPLETED', CURRENT_TIMESTAMP(), 80000); 