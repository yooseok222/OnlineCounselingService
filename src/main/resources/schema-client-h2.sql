-- ========================================
-- 클라이언트 전용 추가 스키마 (H2)
-- ========================================

-- Stamp 테이블 생성
CREATE TABLE IF NOT EXISTS stamp (
    stamp_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    stamp_image_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT stamp_client_id_fk FOREIGN KEY (client_id) REFERENCES client(client_id),
    CONSTRAINT stamp_client_id_uk UNIQUE (client_id)
);

-- Stamp 테이블 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_stamp_client_id ON stamp(client_id);
CREATE INDEX IF NOT EXISTS idx_stamp_is_active ON stamp(is_active);

-- Invitation 테이블 생성
CREATE TABLE IF NOT EXISTS invitation (
    invitation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    invitation_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    is_used BOOLEAN DEFAULT FALSE,
    CONSTRAINT invitation_contract_id_fk FOREIGN KEY (contract_id) REFERENCES contract(contract_id),
    CONSTRAINT invitation_code_uk UNIQUE (invitation_code)
);

-- Invitation 테이블 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_invitation_contract_id ON invitation(contract_id);
CREATE INDEX IF NOT EXISTS idx_invitation_code ON invitation(invitation_code);
CREATE INDEX IF NOT EXISTS idx_invitation_is_used ON invitation(is_used);

-- Contract Templates 테이블 생성 (계약서 템플릿)
CREATE TABLE IF NOT EXISTS contract_templates (
    contract_template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_name VARCHAR(100) NOT NULL,
    template_content CLOB NOT NULL,
    company_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT ct_company_id_fk FOREIGN KEY (company_id) REFERENCES company(company_id)
);

-- Contract Templates 테이블 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_ct_company_id ON contract_templates(company_id);
CREATE INDEX IF NOT EXISTS idx_ct_is_active ON contract_templates(is_active);

-- Contract 테이블이 없다면 생성 (기존 스키마에 없을 경우를 대비)
CREATE TABLE IF NOT EXISTS contract (
    contract_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20) DEFAULT '진행중' NOT NULL,
    created_at TIMESTAMP NOT NULL,
    contract_time TIMESTAMP,
    client_id BIGINT,
    agent_id BIGINT,
    contract_template_id BIGINT,
    memo CLOB,
    company_id BIGINT
);

-- Contract 테이블 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_contract_status ON contract(status);
CREATE INDEX IF NOT EXISTS idx_contract_client_id ON contract(client_id);
CREATE INDEX IF NOT EXISTS idx_contract_agent_id ON contract(agent_id); 