-- 회사 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM company WHERE company_id = 1;
    IF v_count = 0 THEN
      INSERT INTO company (company_id, company_name, created_at)
      VALUES (1, '비상(Visang) 주식회사', CURRENT_TIMESTAMP);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO company (company_id, company_name, created_at)
      VALUES (1, '비상(Visang) 주식회사', CURRENT_TIMESTAMP);
  END;
END;
/

-- 관리자 데이터
-- password: @Pwd2025
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM admin WHERE admin_id = 1;
    IF v_count = 0 THEN
      INSERT INTO admin (admin_id, company_id, name, email, password, phone_number, adress, role, created_at, email_verified)
      VALUES (1, 1, '관리자', 'admin@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-1234-5678', '서울시 강남구 테헤란로 123', 'ADMIN', CURRENT_TIMESTAMP, 1);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO admin (admin_id, company_id, name, email, password, phone_number, adress, role, created_at, email_verified)
      VALUES (1, 1, '관리자', 'admin@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-1234-5678', '서울시 강남구 테헤란로 123', 'ADMIN', CURRENT_TIMESTAMP, 1);
  END;
END;
/

-- 상담원 데이터
-- password: @Pwd2025
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM agent WHERE agent_id = 1;
    IF v_count = 0 THEN
      INSERT INTO agent (agent_id, company_id, name, email, password, phone_number, address, role, state, created_at, profile_image_url, email_verified)
      VALUES (1, 1, '상담원', 'agent@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-2345-6789', '서울시 서초구 강남대로 456', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, NULL, 1);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO agent (agent_id, company_id, name, email, password, phone_number, address, role, state, created_at, profile_image_url, email_verified)
      VALUES (1, 1, '상담원', 'agent@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-2345-6789', '서울시 서초구 강남대로 456', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, NULL, 1);
  END;
END;
/

-- 고객 데이터
-- password: @Pwd2025
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM client WHERE client_id = 1;
    IF v_count = 0 THEN
      INSERT INTO client (client_id, ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified)
      VALUES (1, '910101-1234567', '고객', 'user@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-3456-7890', '서울시 송파구 올림픽로 789', 'USER', CURRENT_TIMESTAMP, NULL, 1);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO client (client_id, ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified)
      VALUES (1, '910101-1234567', '고객', 'user@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-3456-7890', '서울시 송파구 올림픽로 789', 'USER', CURRENT_TIMESTAMP, NULL, 1);
  END;
END;
/

-- 추가 고객 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM client WHERE client_id = 2;
    IF v_count = 0 THEN
      INSERT INTO client (client_id, ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified)
      VALUES (2, '920202-2345678', '김철수', 'kim@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-4567-8901', '서울시 강동구 성내로 100', 'USER', CURRENT_TIMESTAMP - 30, NULL, 1);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM client WHERE client_id = 3;
    IF v_count = 0 THEN
      INSERT INTO client (client_id, ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified)
      VALUES (3, '930303-1456789', '이영희', 'lee@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-5678-9012', '서울시 서대문구 연세로 50', 'USER', CURRENT_TIMESTAMP - 60, NULL, 1);
    END IF;
  END;
END;
/

-- 추가 상담원 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM agent WHERE agent_id = 2;
    IF v_count = 0 THEN
      INSERT INTO agent (agent_id, company_id, name, email, password, phone_number, address, role, state, created_at, profile_image_url, email_verified)
      VALUES (2, 1, '박상담', 'agent2@example.com', '$2a$10$pl/CWsmAOd4Mw6b7oRY4L.XD23Xr0yAeUm0vRu7Y1XGDCjUE922oy', '010-6789-0123', '서울시 마포구 월드컵로 240', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP - 90, NULL, 1);
    END IF;
  END;
END;
/

-- 계약 템플릿 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract_templates WHERE contract_template_id = 1;
    IF v_count = 0 THEN
      INSERT INTO contract_templates (contract_template_id, contract_name, descript, file_path, company_id, version, created_at, updated_at)
      VALUES (1, '표준 보험 계약서', '일반 보험 상품용 표준 계약서 템플릿', '/templates/standard_insurance.pdf', 1, '1.0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract_templates WHERE contract_template_id = 2;
    IF v_count = 0 THEN
      INSERT INTO contract_templates (contract_template_id, contract_name, descript, file_path, company_id, version, created_at, updated_at)
      VALUES (2, '자동차 보험 계약서', '자동차 보험 전용 계약서 템플릿', '/templates/car_insurance.pdf', 1, '1.0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
  END;
END;
/

-- 계약 더미 데이터
-- 오늘 예정된 계약들
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 1;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (1, 1, 1, 'PENDING', CURRENT_TIMESTAMP, '생명보험 상담 예정', 1, 1, CURRENT_TIMESTAMP + INTERVAL '2' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 2;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (2, 2, 1, 'PENDING', CURRENT_TIMESTAMP, '자동차 보험 갱신 상담', 2, 1, CURRENT_TIMESTAMP + INTERVAL '6' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 3;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (3, 3, 2, 'PENDING', CURRENT_TIMESTAMP, '종합보험 신규 가입 상담', 1, 1, CURRENT_TIMESTAMP + INTERVAL '8' HOUR);
    END IF;
  END;
END;
/

-- 진행 중인 계약
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 4;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (4, 1, 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - 7, '연금보험 가입 진행 중', 1, 1, CURRENT_TIMESTAMP - 7 + INTERVAL '3' HOUR);
    END IF;
  END;
END;
/

-- 완료된 계약
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 5;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (5, 1, 1, 'COMPLETED', CURRENT_TIMESTAMP - 30, '화재보험 계약 완료', 1, 1, CURRENT_TIMESTAMP - 30 + INTERVAL '7' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 6;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (6, 2, 2, 'COMPLETED', CURRENT_TIMESTAMP - 60, '실손보험 계약 완료', 1, 1, CURRENT_TIMESTAMP - 60 + INTERVAL '5' HOUR);
    END IF;
  END;
END;
/

-- 취소된 계약
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 7;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (7, 3, 1, 'CANCELLED', CURRENT_TIMESTAMP - 15, '고객 요청으로 취소', 2, 1, CURRENT_TIMESTAMP - 15 + INTERVAL '1' HOUR);
    END IF;
  END;
END;
/

-- 내일 예정된 계약
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 8;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (8, 1, 1, 'PENDING', CURRENT_TIMESTAMP, '치아보험 상담 예정', 1, 1, CURRENT_TIMESTAMP + INTERVAL '1' DAY + INTERVAL '2' HOUR);
    END IF;
  END;
END;
/

-- 오늘 날짜 추가 계약 데이터 (client_id = 1용)
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 9;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (9, 1, 2, 'SCHEDULED', CURRENT_TIMESTAMP, '상해보험 상담 예정', 1, 1, CURRENT_TIMESTAMP + INTERVAL '3' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 10;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (10, 1, 1, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, '여행자 보험 상담 진행 중', 2, 1, CURRENT_TIMESTAMP + INTERVAL '1' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 11;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (11, 1, 2, 'PENDING', CURRENT_TIMESTAMP, '펫 보험 신규 가입 상담', 1, 1, CURRENT_TIMESTAMP + INTERVAL '7' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 12;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (12, 1, 1, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, '암 보험 계약 완료', 1, 1, CURRENT_TIMESTAMP);
    END IF;
  END;
END;
/

-- 과거 계약 데이터 추가 (대시보드 통계용)
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 13;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (13, 1, 2, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '3' DAY, '실버 보험 계약 완료', 1, 1, CURRENT_TIMESTAMP - INTERVAL '3' DAY + INTERVAL '6' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 14;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (14, 1, 1, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '5' DAY, '어린이 보험 계약 완료', 2, 1, CURRENT_TIMESTAMP - INTERVAL '5' DAY + INTERVAL '2' HOUR);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE contract_id = 15;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, client_id, agent_id, status, created_at, memo, contract_template_id, company_id, contract_time)
      VALUES (15, 1, 2, 'CANCELLED', CURRENT_TIMESTAMP - INTERVAL '10' DAY, '일정 변경으로 취소', 1, 1, CURRENT_TIMESTAMP - INTERVAL '10' DAY + INTERVAL '8' HOUR);
    END IF;
  END;
END;
/ 