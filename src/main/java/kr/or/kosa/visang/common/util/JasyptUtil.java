package kr.or.kosa.visang.common.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jasypt 암호화/복호화 유틸리티 클래스
 */
public class JasyptUtil {
    private static final Logger logger = LoggerFactory.getLogger(JasyptUtil.class);
    
    /**
     * Jasypt로 암호화된 값을 복호화
     * 복호화 실패 시 테스트용 기본값 반환
     */
    public static String getDecryptedValue(String encryptedValue, String password) {
        try {
            // 암호화된 값이 아니면 그대로 반환
            if (encryptedValue == null || !encryptedValue.startsWith("ENC(") || !encryptedValue.endsWith(")")) {
                return encryptedValue;
            }
            
            // ENC() 형식에서 실제 암호화된 텍스트 추출
            String encryptedText = encryptedValue.substring(4, encryptedValue.length() - 1);
            
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setAlgorithm("PBEWithMD5AndDES");
            encryptor.setPassword(password);
            
            return encryptor.decrypt(encryptedText);
        } catch (EncryptionOperationNotPossibleException e) {
            logger.warn("복호화 실패. 개발 환경용 테스트 계정을 사용합니다: {}", e.getMessage());
            // 기본값 반환 (복호화 실패 시)
            return "test@example.com";
        } catch (Exception e) {
            logger.error("암호화 처리 중 오류 발생: {}", e.getMessage());
            return "dev-test-value";
        }
    }
    
    /**
     * 문자열 암호화
     */
    public static String encrypt(String value, String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setPassword(password);
        return encryptor.encrypt(value);
    }
} 