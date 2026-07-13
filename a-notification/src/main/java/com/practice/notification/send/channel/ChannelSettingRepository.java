package com.practice.notification.send.channel;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 채널 설정 저장소입니다. H2 파일모드에 저장됩니다(원본도 로컬은 H2).
 */
public interface ChannelSettingRepository
        extends JpaRepository<ChannelSetting, ChannelSetting.Key> {
}
