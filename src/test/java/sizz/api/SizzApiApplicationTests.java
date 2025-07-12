package sizz.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

@ActiveProfiles("test")
@SpringBootTest
@Import(SizzApiApplicationTests.MockConfig.class)
class SizzApiApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public RedisTemplate<String, Object> redisTemplate() {
            return mock(RedisTemplate.class);
        }
    }
}
