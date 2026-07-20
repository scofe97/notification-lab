package com.practice.notification.send.channel;

import com.practice.notification.send.domain.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 수신 설정을 저장하고 캐시를 새 값으로 갱신합니다(FR-12).
     *
     * <p><b>왜 {@code @CachePut}인가</b>: {@code @Cacheable}은 원본 변경을 감지하지 못합니다. 저장 경로에서
     * 캐시를 손대지 않으면 TTL(10분)이 만료될 때까지 발송 파이프라인이 옛 설정을 씁니다. 사용자가 수신 거부를
     * 눌러도 한동안 알림이 계속 가는 상태입니다.
     *
     * <p>{@code @CacheEvict}로 지우기만 해도 되지만, 그러면 다음 발송이 캐시 미스로 DB를 한 번 더 칩니다.
     * 저장 시점에 이미 정확한 값을 알고 있으므로 그 값을 그대로 캐시에 써 넣습니다. 키는
     * {@code isEnabled}와 반드시 같은 형식이어야 하며, 다르면 갱신이 아니라 별도 항목이 생겨 무효화가 실패합니다.
     */
    @CachePut(cacheNames = "channelSetting", key = "#userId + ':' + #channelType")
    @Transactional
    public boolean save(String userId, ChannelType channelType, boolean enabled) {
        repository.save(new ChannelSetting(userId, channelType, enabled));
        log.debug("채널 설정 저장: userId={}, channel={}, enabled={}", userId, channelType, enabled);

        return enabled;
    }
}
