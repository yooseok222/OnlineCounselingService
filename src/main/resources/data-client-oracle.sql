-- ========================================
-- 클라이언트 전용 추가 데이터 (Oracle)
-- ========================================

-- Contract Templates 더미 데이터 (비상 회사용)
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract_templates WHERE contract_template_id = 1;
    IF v_count = 0 THEN
      INSERT INTO contract_templates (contract_template_id, contract_name, template_content, company_id, created_at, is_active)
      VALUES (contract_template_seq.NEXTVAL, '표준 온라인 상담 계약서', 
      '<h2>온라인 상담 서비스 계약서</h2>
      <p>본 계약서는 비상(Visang) 주식회사(이하 "회사")와 고객(이하 "고객") 간의 온라인 상담 서비스 이용에 관한 계약입니다.</p>
      <h3>제1조 (서비스 내용)</h3>
      <p>회사는 고객에게 온라인 상담 서비스를 제공합니다.</p>
      <h3>제2조 (이용 요금)</h3>
      <p>상담 서비스 이용 요금은 시간당 50,000원입니다.</p>
      <h3>제3조 (계약 기간)</h3>
      <p>본 계약은 서명일로부터 1년간 유효합니다.</p>', 
      1, CURRENT_TIMESTAMP, 1);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract_templates WHERE contract_template_id = 2;
    IF v_count = 0 THEN
      INSERT INTO contract_templates (contract_template_id, contract_name, template_content, company_id, created_at, is_active)
      VALUES (contract_template_seq.NEXTVAL, '프리미엄 상담 계약서', 
      '<h2>프리미엄 온라인 상담 서비스 계약서</h2>
      <p>본 계약서는 비상(Visang) 주식회사의 프리미엄 상담 서비스에 관한 계약입니다.</p>
      <h3>제1조 (서비스 내용)</h3>
      <p>전문 상담사의 1:1 맞춤형 상담 서비스를 제공합니다.</p>
      <h3>제2조 (이용 요금)</h3>
      <p>프리미엄 서비스 이용 요금은 시간당 80,000원입니다.</p>', 
      1, CURRENT_TIMESTAMP, 1);
    END IF;
  END;
END;
/

-- 기존 고객의 도장 등록 더미 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM stamp WHERE client_id = 1;
    IF v_count = 0 THEN
      INSERT INTO stamp (stamp_id, client_id, stamp_image_url, created_at, is_active)
      VALUES (stamp_seq.NEXTVAL, 1, '/upload/stamps/stamp_1_sample.png', CURRENT_TIMESTAMP, 1);
    END IF;
  END;
END;
/

-- 예약된 계약 더미 데이터 (오늘 날짜 기준)
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE client_id = 1 AND status = 'PENDING' AND contract_time BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '2' HOUR;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
      VALUES (contract_seq.NEXTVAL, 'PENDING', CURRENT_TIMESTAMP, 
              CURRENT_TIMESTAMP + INTERVAL '2' HOUR, 1, 1, 1, '첫 상담 예약', 1);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE client_id = 1 AND status = 'PENDING' AND contract_time BETWEEN CURRENT_TIMESTAMP + INTERVAL '1' DAY AND CURRENT_TIMESTAMP + INTERVAL '1' DAY + INTERVAL '15' HOUR;
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
      VALUES (contract_seq.NEXTVAL, 'PENDING', CURRENT_TIMESTAMP, 
              CURRENT_TIMESTAMP + INTERVAL '1' DAY + INTERVAL '14' HOUR, 1, 1, 1, '정기 상담', 1);
    END IF;
  END;
END;
/

-- 완료된 계약 더미 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE client_id = 1 AND status = 'COMPLETED' AND memo = '프리미엄 상담 완료';
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
      VALUES (contract_seq.NEXTVAL, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '7' DAY, 
              CURRENT_TIMESTAMP - INTERVAL '7' DAY + INTERVAL '10' HOUR, 1, 1, 2, '프리미엄 상담 완료', 1);
    END IF;
  END;
END;
/

BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE client_id = 1 AND status = 'COMPLETED' AND memo = '일반 상담 완료';
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
      VALUES (contract_seq.NEXTVAL, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '14' DAY, 
              CURRENT_TIMESTAMP - INTERVAL '14' DAY + INTERVAL '15' HOUR, 1, 1, 1, '일반 상담 완료', 1);
    END IF;
  END;
END;
/

-- 취소된 계약 더미 데이터
BEGIN
  DECLARE v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM contract WHERE client_id = 1 AND status = 'CANCELED' AND memo = '고객 요청으로 취소';
    IF v_count = 0 THEN
      INSERT INTO contract (contract_id, status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
      VALUES (contract_seq.NEXTVAL, 'CANCELED', CURRENT_TIMESTAMP - INTERVAL '3' DAY, 
              CURRENT_TIMESTAMP - INTERVAL '2' DAY, 1, 1, 1, '고객 요청으로 취소', 1);
    END IF;
  END;
END;
/

-- 초대 코드 더미 데이터 (예약된 계약용)
BEGIN
  -- 모든 PENDING 상태 계약에 대해 초대 코드 생성
  FOR rec IN (SELECT contract_id, created_at, contract_time 
              FROM contract 
              WHERE status = 'PENDING' 
              AND NOT EXISTS (SELECT 1 FROM invitation WHERE invitation.contract_id = contract.contract_id))
  LOOP
    INSERT INTO invitation (invitation_id, contract_id, invitation_code, created_at, expired_at, is_used)
    VALUES (invitation_seq.NEXTVAL, rec.contract_id, 
            SUBSTR(DBMS_RANDOM.STRING('X', 6), 1, 6),
            rec.created_at,
            rec.contract_time + INTERVAL '1' HOUR,
            0);
  END LOOP;
  COMMIT;
END;
/ 