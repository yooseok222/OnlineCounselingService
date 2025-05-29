package kr.or.kosa.visang.domain.contract.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 세션과 계약 ID 매핑 관리 서비스
 * WebSocket 세션과 contract_id 간의 매핑을 Redis에서 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionContractMappingService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key Prefix
    private static final String SESSION_MAPPING_PREFIX = "session:mapping:";
    private static final String CONTRACT_SESSION_PREFIX = "contract:session:";

    @Value("${app.session.mapping.ttl:24}")
    private int sessionMappingTtlHours;

    /**
     * 세션-계약 매핑 생성
     * @param sessionId WebSocket 세션 ID
     * @param contractId 계약 ID
     * @param role 사용자 역할 (agent/client)
     */
    public void createMapping(String sessionId, Long contractId, String role) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(sessionMappingTtlHours);

            // 매핑 데이터 생성
            Map<String, Object> mappingData = new HashMap<>();
            mappingData.put("contractId", contractId);
            mappingData.put("createdAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            mappingData.put("expiresAt", expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            mappingData.put("role", role);
            mappingData.put("lastAccessAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // 1. 세션 → 계약 매핑 저장
            String sessionKey = SESSION_MAPPING_PREFIX + sessionId;
            redisTemplate.opsForValue().set(sessionKey, mappingData, sessionMappingTtlHours, TimeUnit.HOURS);

            // 2. 계약 → 세션 매핑 저장 (역방향 조회용)
            String contractKey = CONTRACT_SESSION_PREFIX + contractId;
            redisTemplate.opsForValue().set(contractKey, sessionId, sessionMappingTtlHours, TimeUnit.HOURS);

            log.info("세션-계약 매핑 생성 완료: 세션={}, 계약={}, 역할={}", sessionId, contractId, role);

        } catch (Exception e) {
            log.error("세션-계약 매핑 생성 실패: 세션={}, 계약={}", sessionId, contractId, e);
            throw new RuntimeException("세션 매핑 생성에 실패했습니다.", e);
        }
    }

    /**
     * 세션으로 계약 ID 조회
     * @param sessionId WebSocket 세션 ID
     * @return 계약 ID (없으면 null)
     */
    public Long getContractIdBySession(String sessionId) {
        try {
            String sessionKey = SESSION_MAPPING_PREFIX + sessionId;
            Object mappingObj = redisTemplate.opsForValue().get(sessionKey);

            if (mappingObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapping = (Map<String, Object>) mappingObj;
                Object contractIdObj = mapping.get("contractId");
                
                if (contractIdObj instanceof Number) {
                    return ((Number) contractIdObj).longValue();
                }
            }

            log.debug("세션에 대한 계약 ID를 찾을 수 없음: 세션={}", sessionId);
            return null;

        } catch (Exception e) {
            log.error("세션으로 계약 ID 조회 실패: 세션={}", sessionId, e);
            return null;
        }
    }

    /**
     * 계약 ID로 활성 세션 조회
     * @param contractId 계약 ID
     * @return 세션 ID (없으면 null)
     */
    public String getSessionByContractId(Long contractId) {
        try {
            String contractKey = CONTRACT_SESSION_PREFIX + contractId;
            Object sessionObj = redisTemplate.opsForValue().get(contractKey);

            if (sessionObj instanceof String) {
                return (String) sessionObj;
            }

            log.debug("계약에 대한 활성 세션을 찾을 수 없음: 계약={}", contractId);
            return null;

        } catch (Exception e) {
            log.error("계약 ID로 세션 조회 실패: 계약={}", contractId, e);
            return null;
        }
    }

    /**
     * 세션 정보 조회 (전체 매핑 데이터)
     * @param sessionId WebSocket 세션 ID
     * @return 매핑 데이터 (없으면 null)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSessionMapping(String sessionId) {
        try {
            String sessionKey = SESSION_MAPPING_PREFIX + sessionId;
            Object mappingObj = redisTemplate.opsForValue().get(sessionKey);

            if (mappingObj instanceof Map) {
                return (Map<String, Object>) mappingObj;
            }

            return null;

        } catch (Exception e) {
            log.error("세션 매핑 조회 실패: 세션={}", sessionId, e);
            return null;
        }
    }

    /**
     * 세션 매핑 검증
     * @param sessionId WebSocket 세션 ID
     * @param contractId 계약 ID
     * @return 유효한 매핑인지 여부
     */
    public boolean isValidMapping(String sessionId, Long contractId) {
        try {
            Long mappedContractId = getContractIdBySession(sessionId);
            boolean isValid = mappedContractId != null && mappedContractId.equals(contractId);
            
            log.debug("세션 매핑 검증: 세션={}, 계약={}, 유효={}", sessionId, contractId, isValid);
            return isValid;

        } catch (Exception e) {
            log.error("세션 매핑 검증 실패: 세션={}, 계약={}", sessionId, contractId, e);
            return false;
        }
    }

    /**
     * 세션 매핑 삭제
     * @param sessionId WebSocket 세션 ID
     */
    public void removeMapping(String sessionId) {
        try {
            // 먼저 계약 ID 조회
            Long contractId = getContractIdBySession(sessionId);

            // 1. 세션 → 계약 매핑 삭제
            String sessionKey = SESSION_MAPPING_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);

            // 2. 계약 → 세션 매핑 삭제
            if (contractId != null) {
                String contractKey = CONTRACT_SESSION_PREFIX + contractId;
                redisTemplate.delete(contractKey);
            }

            log.info("세션 매핑 삭제 완료: 세션={}, 계약={}", sessionId, contractId);

        } catch (Exception e) {
            log.error("세션 매핑 삭제 실패: 세션={}", sessionId, e);
        }
    }

    /**
     * 세션 활동 시간 업데이트
     * @param sessionId WebSocket 세션 ID
     */
    public void updateLastAccess(String sessionId) {
        try {
            Map<String, Object> mapping = getSessionMapping(sessionId);
            if (mapping != null) {
                mapping.put("lastAccessAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                String sessionKey = SESSION_MAPPING_PREFIX + sessionId;
                redisTemplate.opsForValue().set(sessionKey, mapping, sessionMappingTtlHours, TimeUnit.HOURS);
                
                log.debug("세션 활동 시간 업데이트: 세션={}", sessionId);
            }

        } catch (Exception e) {
            log.error("세션 활동 시간 업데이트 실패: 세션={}", sessionId, e);
        }
    }

    /**
     * 만료된 매핑 정리 (스케줄러에서 호출)
     */
    public void cleanupExpiredMappings() {
        // Redis TTL에 의해 자동으로 만료되므로 별도 정리 불필요
        // 필요시 추가 정리 로직 구현
        log.debug("만료된 세션 매핑 정리 작업 실행");
    }
} 