package sizz.api.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import sizz.api.search.dto.InsightDto;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientCfg = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .shutdownTimeout(Duration.ofMillis(300))
                .build();

        RedisStandaloneConfiguration serverCfg = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(serverCfg, clientCfg);
    }

    /** Redis 전용 ObjectMapper (JavaTime 지원) */
    private ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 1) 공용 제네릭 템플릿 (Object 저장)
     *    키: 문자열, 값: Generic JSON (타입정보 포함)
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper om = redisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(om);

        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);

        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(serializer);
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setHashValueSerializer(serializer);

        t.afterPropertiesSet();
        return t;
    }

    /**
     * 문자열 전용 템플릿 — 카운터/토큰 등 (INCR/GET에 최적)
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(factory);
        t.afterPropertiesSet();
        return t;
    }

    /**
     * InsightDto 전용 템플릿 — 타입 안전 + JavaTime 지원
     */
    @Bean(name = "insightRedisTemplate")
    public RedisTemplate<String, InsightDto> insightRedisTemplate(RedisConnectionFactory factory) {
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer ser = new GenericJackson2JsonRedisSerializer(om);

        RedisTemplate<String, InsightDto> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(ser);
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setHashValueSerializer(ser);
        t.afterPropertiesSet();
        return t;
    }
}