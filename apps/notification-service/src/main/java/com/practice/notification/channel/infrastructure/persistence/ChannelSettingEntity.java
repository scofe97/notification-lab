package com.practice.notification.channel.infrastructure.persistence;

import com.practice.notification.channel.domain.model.ChannelSetting;
import com.practice.notification.send.domain.ChannelType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채널 설정 JPA 엔티티입니다 — outbound adapter의 영속 모델.
 *
 * <p>도메인 모델({@link ChannelSetting})과 분리해, JPA 제약(기본 생성자·복합키 클래스)이
 * 도메인으로 새지 않게 합니다. 테이블명은 분리 전 클래스명(ChannelSetting)이 만들던
 * {@code channel_setting}을 그대로 고정해 기존 H2 데이터를 보존합니다.
 *
 * <p>한 사용자가 채널별로 여러 행을 가지므로 복합키(userId + channelType)를 씁니다.
 */
@Entity
@Table(name = "channel_setting")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ChannelSettingEntity.Key.class)
public class ChannelSettingEntity {

    @Id
    private String userId;

    @Id
    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    private boolean enabled;

    static ChannelSettingEntity from(ChannelSetting setting) {
        return new ChannelSettingEntity(setting.getUserId(), setting.getChannelType(), setting.isEnabled());
    }

    ChannelSetting toDomain() {
        return new ChannelSetting(userId, channelType, enabled);
    }

    /** 복합키 클래스 (userId + channelType). */
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private String userId;
        private ChannelType channelType;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return java.util.Objects.equals(userId, key.userId)
                    && channelType == key.channelType;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, channelType);
        }
    }
}
