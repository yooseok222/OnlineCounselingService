-- ========================================
-- 클라이언트 전용 추가 데이터 (H2)
-- ========================================

-- Contract Templates 더미 데이터 (비상 회사용)
INSERT INTO contract_templates (contract_name, template_content, company_id, created_at, is_active)
VALUES ('표준 온라인 상담 계약서', 
'<h2>온라인 상담 서비스 계약서</h2>
<p>본 계약서는 비상(Visang) 주식회사(이하 "회사")와 고객(이하 "고객") 간의 온라인 상담 서비스 이용에 관한 계약입니다.</p>
<h3>제1조 (서비스 내용)</h3>
<p>회사는 고객에게 온라인 상담 서비스를 제공합니다.</p>
<h3>제2조 (이용 요금)</h3>
<p>상담 서비스 이용 요금은 시간당 50,000원입니다.</p>
<h3>제3조 (계약 기간)</h3>
<p>본 계약은 서명일로부터 1년간 유효합니다.</p>', 
1, CURRENT_TIMESTAMP(), TRUE);

INSERT INTO contract_templates (contract_name, template_content, company_id, created_at, is_active)
VALUES ('프리미엄 상담 계약서', 
'<h2>프리미엄 온라인 상담 서비스 계약서</h2>
<p>본 계약서는 비상(Visang) 주식회사의 프리미엄 상담 서비스에 관한 계약입니다.</p>
<h3>제1조 (서비스 내용)</h3>
<p>전문 상담사의 1:1 맞춤형 상담 서비스를 제공합니다.</p>
<h3>제2조 (이용 요금)</h3>
<p>프리미엄 서비스 이용 요금은 시간당 80,000원입니다.</p>', 
1, CURRENT_TIMESTAMP(), TRUE);

-- 기존 고객의 도장 등록 더미 데이터
INSERT INTO stamp (client_id, stamp_image_url, created_at, is_active)
VALUES (1, '/upload/stamps/stamp_1_sample.png', CURRENT_TIMESTAMP(), TRUE);

-- 예약된 계약 더미 데이터 (오늘 날짜 기준)
INSERT INTO contract (status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
VALUES ('PENDING', CURRENT_TIMESTAMP(), 
        DATEADD(HOUR, 2, CURRENT_TIMESTAMP()), 1, 1, 1, '첫 상담 예약', 1);

INSERT INTO contract (status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
VALUES ('PENDING', CURRENT_TIMESTAMP(), 
        DATEADD(HOUR, 38, CURRENT_TIMESTAMP()), 1, 1, 1, '정기 상담', 1);

-- 완료된 계약 더미 데이터
INSERT INTO contract (status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
VALUES ('COMPLETED', DATEADD(DAY, -7, CURRENT_TIMESTAMP()), 
        DATEADD(HOUR, 10, DATEADD(DAY, -7, CURRENT_TIMESTAMP())), 1, 1, 2, '프리미엄 상담 완료', 1);

INSERT INTO contract (status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
VALUES ('COMPLETED', DATEADD(DAY, -14, CURRENT_TIMESTAMP()), 
        DATEADD(HOUR, 15, DATEADD(DAY, -14, CURRENT_TIMESTAMP())), 1, 1, 1, '일반 상담 완료', 1);

-- 취소된 계약 더미 데이터
INSERT INTO contract (status, created_at, contract_time, client_id, agent_id, contract_template_id, memo, company_id)
VALUES ('CANCELED', DATEADD(DAY, -3, CURRENT_TIMESTAMP()), 
        DATEADD(DAY, -2, CURRENT_TIMESTAMP()), 1, 1, 1, '고객 요청으로 취소', 1);

-- 초대 코드 더미 데이터 (예약된 계약용)
-- H2에서는 RANDOM_UUID() 함수를 사용하여 랜덤 코드 생성
INSERT INTO invitation (contract_id, invitation_code, created_at, expired_at, is_used)
SELECT c.contract_id, 
       UPPER(SUBSTRING(CAST(RANDOM_UUID() AS VARCHAR), 1, 6)),
       c.created_at,
       DATEADD(HOUR, 1, c.contract_time),
       FALSE
FROM contract c
WHERE c.status = 'PENDING' 
ORDER BY c.contract_id DESC
LIMIT 2; 