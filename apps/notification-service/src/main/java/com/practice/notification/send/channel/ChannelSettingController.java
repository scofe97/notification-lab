package com.practice.notification.send.channel;

import com.practice.notification.send.domain.ChannelType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림채널 설정 REST 계약입니다(UC-4, FR-12).
 *
 * <p>사용자가 채널별 수신 여부를 조회(GET)하거나 저장(PUT)합니다. 화면은 만들지 않고 계약만 둡니다.
 *
 * <p>저장한 설정은 UC-1 발송 파이프라인이 캐시를 거쳐 읽습니다. 저장 시 캐시 갱신은
 * {@link ChannelSettingService#save}가 담당합니다.
 */
@RestController
@RequestMapping("/api/users/{userId}/channels/{channelType}")
@RequiredArgsConstructor
public class ChannelSettingController {

    private final ChannelSettingService channelSettingService;

    /**
     * 수신 설정을 조회합니다. 저장된 설정이 없으면 기본값 {@code true}(수신)로 응답합니다.
     */
    @GetMapping
    public ChannelSettingResponse get(@PathVariable String userId,
                                      @PathVariable ChannelType channelType) {
        boolean enabled = channelSettingService.isEnabled(userId, channelType);

        return ChannelSettingResponse.of(userId, channelType, enabled);
    }

    /**
     * 수신 설정을 저장합니다. 없으면 생성하고 있으면 덮어씁니다(upsert).
     */
    @PutMapping
    public ChannelSettingResponse put(@PathVariable String userId,
                                      @PathVariable ChannelType channelType,
                                      @Valid @RequestBody ChannelSettingUpdateRequest request) {
        boolean enabled = channelSettingService.save(userId, channelType, request.enabled());

        return ChannelSettingResponse.of(userId, channelType, enabled);
    }
}
