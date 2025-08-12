package kr.or.kosa.visang.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import kr.or.kosa.visang.domain.company.dto.InvitationResponse;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 개발 환경(dev, h2, oracle 프로필)에서 테스트용 초대코드(LOVE)를 자동으로 Redis 에 삽입한다.
 * 회사 ID 1, 회사명 "비상", 관리자명 "관리자" 기준.
 * 만료 기간은 7일로 설정.
 * 서버 재시작 시 항상 초기화하여 최신 상태로 유지합니다.
 */
@Slf4j
@Configuration
@Profile({"dev", "h2", "oracle"})
@RequiredArgsConstructor
public class RedisDevDummyDataConfig {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long EXPIRATION_DAYS = 7;
    private static final String FIXED_CODE = "LOVE"; // 고정 테스트 초대코드

    /**
     * 애플리케이션 기동 후 테스트용 초대코드를 하나 생성한다.
     * 기존에 동일한 코드가 있으면 삭제 후 다시 생성하여 항상 최신 상태를 유지한다.
     * 
     * – companyId 1 / adminId 1 기준 (data.sql 에 포함된 기본 데이터)
     */
    @Bean
    @org.springframework.context.annotation.DependsOn("redisFlushRunner")
    @org.springframework.core.annotation.Order(2)
    public ApplicationRunner dummyInvitationLoader() {
        return args -> {
            String detailKey = "invitation:detail:" + FIXED_CODE;
            String adminSet = "invitation:list:byAdmin:1";
            String companySet = "invitation:list:byCompany:1";
            
            // 기존 초대코드 관련 키가 있으면 먼저 삭제
            if (Boolean.TRUE.equals(redisTemplate.hasKey(detailKey))) {
                log.info("[dev] Removing existing dummy invitation code '{}' from Redis for fresh initialization.", FIXED_CODE);
                redisTemplate.delete(detailKey);
                redisTemplate.opsForSet().remove(adminSet, FIXED_CODE);
                redisTemplate.opsForSet().remove(companySet, FIXED_CODE);
            }

            InvitationResponse invitation = InvitationResponse.builder()
                    .invitationId(System.currentTimeMillis())
                    .invitationCode(FIXED_CODE)
                    .companyId(1L)
                    .companyName("비상(Visang) 주식회사")
                    .adminId(1L)
                    .adminName("관리자")
                    .createdAt(java.time.LocalDateTime.now())
                    .expiredTime(java.time.LocalDateTime.now().plusDays(EXPIRATION_DAYS))
                    .expired(false)
                    .build();

            long ttlSecs = java.time.Duration.ofDays(EXPIRATION_DAYS).getSeconds();

            redisTemplate.opsForValue().set(detailKey, invitation, ttlSecs, java.util.concurrent.TimeUnit.SECONDS);

            redisTemplate.opsForSet().add(adminSet, FIXED_CODE);
            redisTemplate.opsForSet().add(companySet, FIXED_CODE);
            redisTemplate.expire(adminSet, ttlSecs, java.util.concurrent.TimeUnit.SECONDS);
            redisTemplate.expire(companySet, ttlSecs, java.util.concurrent.TimeUnit.SECONDS);

            log.info("[dev] Fixed dummy invitation code '{}' created/updated ({} days TTL).", FIXED_CODE, EXPIRATION_DAYS);
        };
    }
} 