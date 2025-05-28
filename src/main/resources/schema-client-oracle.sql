-- ========================================
-- 클라이언트 전용 추가 스키마 (Oracle)
-- ========================================

-- Stamp 테이블 Drop
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE stamp CASCADE CONSTRAINTS';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -942 THEN
      RAISE;
    END IF;
END;
/

-- Invitation 테이블 Drop  
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE invitation CASCADE CONSTRAINTS';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -942 THEN
      RAISE;
    END IF;
END;
/

-- Contract Templates 테이블 Drop
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE contract_templates CASCADE CONSTRAINTS';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -942 THEN
      RAISE;
    END IF;
END;
/

-- 시퀀스 Drop
BEGIN
  EXECUTE IMMEDIATE 'DROP SEQUENCE stamp_seq';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -2289 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'DROP SEQUENCE invitation_seq';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -2289 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'DROP SEQUENCE contract_template_seq';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -2289 THEN
      RAISE;
    END IF;
END;
/

-- 시퀀스 생성
CREATE SEQUENCE stamp_seq START WITH 1 INCREMENT BY 1
/
CREATE SEQUENCE invitation_seq START WITH 1 INCREMENT BY 1
/
CREATE SEQUENCE contract_template_seq START WITH 1 INCREMENT BY 1
/

-- Stamp 테이블 생성
BEGIN
  EXECUTE IMMEDIATE '
  CREATE TABLE stamp (
      stamp_id NUMBER(19) PRIMARY KEY,
      client_id NUMBER(19) NOT NULL,
      stamp_image_url VARCHAR2(255) NOT NULL,
      created_at TIMESTAMP NOT NULL,
      last_used_at TIMESTAMP,
      is_active NUMBER(1) DEFAULT 1,
      CONSTRAINT stamp_client_id_fk FOREIGN KEY (client_id) REFERENCES client(client_id),
      CONSTRAINT stamp_client_id_uk UNIQUE (client_id)
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

-- Stamp 테이블 인덱스 생성
BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_stamp_client_id ON stamp(client_id)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_stamp_is_active ON stamp(is_active)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

-- Invitation 테이블 생성
BEGIN
  EXECUTE IMMEDIATE '
  CREATE TABLE invitation (
      invitation_id NUMBER(19) PRIMARY KEY,
      contract_id NUMBER(19) NOT NULL,
      invitation_code VARCHAR2(20) NOT NULL,
      created_at TIMESTAMP NOT NULL,
      expired_at TIMESTAMP NOT NULL,
      used_at TIMESTAMP,
      is_used NUMBER(1) DEFAULT 0,
      CONSTRAINT invitation_contract_id_fk FOREIGN KEY (contract_id) REFERENCES contract(contract_id),
      CONSTRAINT invitation_code_uk UNIQUE (invitation_code)
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

-- Invitation 테이블 인덱스 생성
BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_invitation_contract_id ON invitation(contract_id)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_invitation_code ON invitation(invitation_code)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_invitation_is_used ON invitation(is_used)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

-- Contract Templates 테이블 생성 (계약서 템플릿)
BEGIN
  EXECUTE IMMEDIATE '
  CREATE TABLE contract_templates (
      contract_template_id NUMBER(19) PRIMARY KEY,
      contract_name VARCHAR2(100) NOT NULL,
      template_content CLOB NOT NULL,
      company_id NUMBER(19) NOT NULL,
      created_at TIMESTAMP NOT NULL,
      updated_at TIMESTAMP,
      is_active NUMBER(1) DEFAULT 1,
      CONSTRAINT ct_company_id_fk FOREIGN KEY (company_id) REFERENCES company(company_id)
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

-- Contract Templates 테이블 인덱스 생성
BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ct_company_id ON contract_templates(company_id)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ct_is_active ON contract_templates(is_active)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/ 