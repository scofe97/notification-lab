package com.practice.notification.channel.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 채널 설정 Spring Data 저장소입니다. H2 파일 모드에 저장됩니다.
 *
 * <p>어댑터 내부 구현이므로 application·domain은 이 인터페이스를 모릅니다.
 */
public interface ChannelSettingJpaRepository
        extends JpaRepository<ChannelSettingEntity, ChannelSettingEntity.Key> {
}
