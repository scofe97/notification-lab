package com.practice.notification.channel.domain.port.out;

import com.practice.notification.channel.domain.model.ChannelSetting;
import com.practice.notification.common.domain.ChannelType;
import java.util.Optional;

/**
 * 채널 설정 영속화 out-port입니다.
 *
 * <p>도메인이 "저장·조회가 필요하다"는 요구를 선언하고, infrastructure의
 * {@code ChannelSettingPersistenceAdapter}가 구현합니다(역의존). 도메인은 저장 기술(JPA·H2)을 모릅니다.
 */
public interface ChannelSettingPort {

    /** (userId, channelType) 조합의 설정을 조회합니다. 저장된 적 없으면 빈 값입니다. */
    Optional<ChannelSetting> find(String userId, ChannelType channelType);

    /** 설정을 저장합니다(upsert). */
    void save(ChannelSetting setting);
}
