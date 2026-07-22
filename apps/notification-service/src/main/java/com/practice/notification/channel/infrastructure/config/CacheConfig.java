package com.practice.notification.channel.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine 캐시 설정입니다. 채널 설정 조회({@code ChannelSettingService.isEnabled})를 캐시합니다(FR-3).
 *
 * <p><b>왜 캐시하나</b>: 발송 파이프라인은 수신자마다 채널 설정을 조회하는데, 설정은 자주 바뀌지 않습니다.
 * 매번 DB를 치면 낭비이므로, (userId, channelType) 조합을 캐시해 같은 조합의 2번째 조회부터는 DB를 건너뜁니다.
 *
 * <p><b>스펙</b>: TTL 10분, 최대 10,000개. 학습용 값이며, 실제로는 설정 변경 빈도에 맞춰 조정합니다.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("channelSetting");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(10_000)
                .recordStats());
        return cacheManager;
    }
}
