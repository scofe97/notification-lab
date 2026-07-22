package com.practice.notification.dispatch.infrastructure.recipient;

import com.practice.notification.dispatch.domain.model.Recipient;
import com.practice.notification.dispatch.domain.port.out.RecipientLookupPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link RecipientLookupPort}의 Feign 구현체입니다 — outbound adapter.
 */
@Component
@RequiredArgsConstructor
public class RecipientLookupAdapter implements RecipientLookupPort {

    private final RecipientLookupClient client;

    @Override
    public List<Recipient> findByGroup(String groupId) {
        return client.findByGroup(groupId).stream()
                .map(RecipientResponse::toDomain)
                .toList();
    }
}
