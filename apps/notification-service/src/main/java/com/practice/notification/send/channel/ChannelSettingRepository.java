package com.practice.notification.send.channel;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 채널 설정 저장소입니다. H2 파일 모드에 저장됩니다.
 */
public interface ChannelSettingRepository
        extends JpaRepository<ChannelSetting, ChannelSetting.Key> {
}
