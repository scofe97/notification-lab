package com.practice.notification.history.infrastructure.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.notification.history.domain.model.NotificationHistory;
import com.practice.notification.history.domain.port.out.ArchiveWriterPort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@link ArchiveWriterPort}의 NDJSON 파일 구현체입니다 — outbound adapter.
 *
 * <p>한 줄에 이력 한 건(JSON)인 NDJSON 형식으로, `notification-history-{날짜}.ndjson`
 * 파일을 만듭니다. 같은 날짜를 재실행하면 덮어써서 재실행에 안전합니다.
 */
@Component
@RequiredArgsConstructor
public class NdjsonArchiveWriter implements ArchiveWriterPort {

    private final ObjectMapper objectMapper;

    @Value("${notification.history.archive-dir}")
    private String archiveDir;

    @Override
    public String write(LocalDate day, List<NotificationHistory> histories) {
        try {
            Path dir = Path.of(archiveDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("notification-history-" + day + ".ndjson");

            StringBuilder lines = new StringBuilder();
            for (NotificationHistory h : histories) {
                lines.append(objectMapper.writeValueAsString(Map.of(
                                "id", h.getId()
                                , "eventId", h.getEventId()
                                , "channelType", h.getChannelType().name()
                                , "destination", h.getDestination()
                                , "succeeded", h.isSucceeded()
                                , "sentAt", h.getSentAt().toString())))
                        .append('\n');
            }
            Files.writeString(file, lines.toString(), StandardCharsets.UTF_8);

            return file.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("아카이브 파일 쓰기 실패: day=" + day, e);
        }
    }
}
