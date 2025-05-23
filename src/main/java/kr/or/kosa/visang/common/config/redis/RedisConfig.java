package kr.or.kosa.visang.common.config.redis;

import kr.or.kosa.visang.domain.chat.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

/**
 * Redis 설정 클래스
 */
@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.redis.flush-on-startup:false}")
    private boolean flushOnStartup;

    /**
     * Redis 연결 팩토리 빈 설정
     *
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(host);
        redisConfiguration.setPort(port);
        return new LettuceConnectionFactory(redisConfiguration);
    }

    /**
     * Redis 템플릿 빈 설정
     * - 초대코드와 같은 키-값 데이터 저장에 사용
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // ObjectMapper: JavaTimeModule 등록
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis DB 초기화 빈
     * spring.redis.flush-on-startup 속성이 true인 경우에만 DB를 초기화
     */
    @Bean(name = "redisFlushRunner")
    public ApplicationRunner redisFlushRunner() {
        return args -> {
            if (flushOnStartup) {
                log.info("Flushing Redis database...");
                RedisConnection connection = redisConnectionFactory().getConnection();
                connection.flushDb();
                connection.close();
                log.info("Redis database has been flushed successfully");
            } else {
                log.info("Redis flush on startup is disabled");
            }
        };
    }

    @Bean("chatRedisTemplate")
    public RedisTemplate<String, ChatMessage> ChatredisTemplate(RedisConnectionFactory rc) {
        RedisTemplate<String, ChatMessage> temp = new RedisTemplate<>();
        temp.setConnectionFactory(rc);
        temp.setKeySerializer(new StringRedisSerializer());
        temp.setValueSerializer(new Jackson2JsonRedisSerializer<>(ChatMessage.class));
        temp.afterPropertiesSet();
        return temp;
    }

} 