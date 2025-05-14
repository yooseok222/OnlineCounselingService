package kr.or.kosa.visang.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class RedisService {
    private final RedisTemplate<String,Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisService(RedisTemplate<String,Object> redisTemplate,
                        StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

//    // ── 단일 값 저장/조회
//    public void saveObject(Long id, Object object) {
//        redisTemplate.opsForValue()
//                .set("object:"+id, object, Duration.ofMinutes(30));
//    }
//    public Object getObject(Long id) {
//        return (Object) redisTemplate.opsForValue().get("object:"+id);
//    }

    // ── 문자열 전용
    public void saveToken(String token, String value) {
        stringRedisTemplate.opsForValue()
                .set("token:"+token, value, Duration.ofHours(1));
    }
    public String getUsernameByToken(String token) {
        return stringRedisTemplate.opsForValue().get("token:"+token);
    }

    // ── 해시
    public void saveProfile(Long id, Map<String,Object> profile) {
        redisTemplate.opsForHash().putAll("profile:"+id, profile);
    }
    public Object getProfileField(Long id, String field) {
        return redisTemplate.opsForHash().get("profile:"+id, field);
    }
}

