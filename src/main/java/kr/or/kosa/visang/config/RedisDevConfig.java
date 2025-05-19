package kr.or.kosa.visang.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 개발(dev) 프로파일에서 애플리케이션 기동 시 Redis DB를 초기화(Flush)한다.
 * H2 인메모리 DB 처럼 실행마다 깨끗한 상태를 보장하기 위함.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class RedisDevConfig {

    private final RedisConnectionFactory connectionFactory;

    /**
     * 애플리케이션 시작 직후 Redis DB를 비운다.
     */
    @Bean
    public ApplicationRunner redisFlushRunner() {
        return args -> {
            try (RedisConnection conn = connectionFactory.getConnection()) {
                conn.flushDb();
                log.info("[dev] Redis DB flushed for a clean development environment.");
            } catch (Exception e) {
                log.warn("[dev] Failed to flush Redis DB: {}", e.getMessage());
            }
        };
    }
} 