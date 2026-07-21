package com.practice.notification.channel.infrastructure;

import com.practice.notification.channel.domain.model.ChannelSetting;
import com.practice.notification.channel.domain.port.out.ChannelSettingPort;
import com.practice.notification.send.domain.ChannelType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link ChannelSettingPort}의 JPA 구현체입니다 — outbound adapter.
 *
 * <p>도메인이 선언한 out-port를 infrastructure가 구현하는 역의존 지점입니다.
 * 엔티티↔도메인 변환은 이 어댑터 경계 안에서 끝냅니다.
 */
@Component
@RequiredArgsConstructor
public class ChannelSettingPersistenceAdapter implements ChannelSettingPort {

    private final ChannelSettingJpaRepository repository;

    @Override
    public Optional<ChannelSetting> find(String userId, ChannelType channelType) {
        return repository.findById(new ChannelSettingEntity.Key(userId, channelType))
                .map(ChannelSettingEntity::toDomain);
    }

    @Override
    public void save(ChannelSetting setting) {
        repository.save(ChannelSettingEntity.from(setting));
    }
}
