package com.practice.notification.send.channel;

import com.practice.notification.send.domain.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 수신자의 채널 수신 여부를 조회합니다. 결과를 Caffeine으로 캐시합니다(FR-3).
 *
 * <p>발송 파이프라인은 수신자마다 이 조회를 하는데, 채널 설정은 자주 바뀌지 않으므로 매번 DB를 치면
 * 낭비입니다. {@code @Cacheable}로 (userId, channelType) 조합을 캐시해, 같은 조합의 2번째 조회부터는
 * DB를 건너뜁니다. 캐시 스펙(TTL·최대 크기)은 {@code CacheConfig}에서 정의합니다.
 *
 * <p>이 캐시는 반복되는 채널 설정 조회의 DB 부하를 줄입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelSettingService {

    private final ChannelSettingRepository repository;

    /**
     * 수신자가 해당 채널을 수신하도록 설정했는지 조회합니다.
     * 설정이 없으면 기본값 {@code true}(수신)로 간주합니다.
     */
    @Cacheable(cacheNames = "channelSetting", key = "#userId + ':' + #channelType")
    public boolean isEnabled(String userId, ChannelType channelType) {
        log.debug("채널 설정 DB 조회 (캐시 미스): userId={}, channel={}", userId, channelType);
        return repository.findById(new ChannelSetting.Key(userId, channelType))
                .map(ChannelSetting::isEnabled)
                .orElse(true);
    }
}
