package com.practice.notification.send.channel;

import com.practice.notification.send.domain.ChannelType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수신자의 채널별 수신 설정입니다.
 *
 * <p>발송 파이프라인은 이 설정을 조회해 "수신 거부한 채널로는 안 보내는" 판단을 합니다.
 * 자주 바뀌지 않으므로 조회 결과를 Caffeine으로 캐시합니다(FR-3).
 *
 * <p>한 사용자가 채널별로 여러 행을 가지므로 복합키(userId + channelType)를 씁니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ChannelSetting.Key.class)
public class ChannelSetting {

    @Id
    private String userId;

    @Id
    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    private boolean enabled;

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
