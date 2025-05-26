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
-- password: @pwd2025
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM admin WHERE admin_id = 1;
    IF v_count = 0 THEN
      INSERT INTO admin (admin_id, company_id, name, email, password, phone_number, address, role, created_at, email_verified)
      VALUES (1, 1, '관리자', 'admin@example.com', '$2a$10$n5xA8Z7QxWvb7RPVQIuW7ORwqJDMF9zL9P./c.Cj.EJqQk33Rlpqm', '010-1234-5678', '서울시 강남구 테헤란로 123', 'ADMIN', CURRENT_TIMESTAMP, 1);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO admin (admin_id, company_id, name, email, password, phone_number, address, role, created_at, email_verified)
      VALUES (1, 1, '관리자', 'admin@example.com', '$2a$10$n5xA8Z7QxWvb7RPVQIuW7ORwqJDMF9zL9P./c.Cj.EJqQk33Rlpqm', '010-1234-5678', '서울시 강남구 테헤란로 123', 'ADMIN', CURRENT_TIMESTAMP, 1);
  END;
END;
/

-- 상담원 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM agent WHERE agent_id = 1;
    IF v_count = 0 THEN
      INSERT INTO agent (agent_id, company_id, name, email, password, phone_number, address, role, state, created_at, profile_image_url, email_verified)
      VALUES (1, 1, '상담원', 'agent@example.com', '$2a$10$n5xA8Z7QxWvb7RPVQIuW7ORwqJDMF9zL9P./c.Cj.EJqQk33Rlpqm', '010-2345-6789', '서울시 서초구 강남대로 456', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, NULL, 1);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO agent (agent_id, company_id, name, email, password, phone_number, address, role, state, created_at, profile_image_url, email_verified)
      VALUES (1, 1, '상담원', 'agent@example.com', '$2a$10$n5xA8Z7QxWvb7RPVQIuW7ORwqJDMF9zL9P./c.Cj.EJqQk33Rlpqm', '010-2345-6789', '서울시 서초구 강남대로 456', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, NULL, 1);
  END;
END;
/

-- 고객 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM client WHERE client_id = 1;
    IF v_count = 0 THEN
      INSERT INTO client (client_id, ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified)
      VALUES (1, '910101-1234567', '고객', 'user@example.com', '$2a$10$n5xA8Z7QxWvb7RPVQIuW7ORwqJDMF9zL9P./c.Cj.EJqQk33Rlpqm', '010-3456-7890', '서울시 송파구 올림픽로 789', 'USER', CURRENT_TIMESTAMP, NULL, 1);
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO client (client_id, ssn, name, email, password, phone_number, address, role, created_at, profile_image_url, email_verified)
      VALUES (1, '910101-1234567', '고객', 'user@example.com', '$2a$10$n5xA8Z7QxWvb7RPVQIuW7ORwqJDMF9zL9P./c.Cj.EJqQk33Rlpqm', '010-3456-7890', '서울시 송파구 올림픽로 789', 'USER', CURRENT_TIMESTAMP, NULL, 1);
  END;
END;
/ 