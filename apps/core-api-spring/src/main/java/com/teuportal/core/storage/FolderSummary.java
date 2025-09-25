package com.teuportal.core.storage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FolderSummary(
        UUID id,
        String name,
        UUID parentId,
        OffsetDateTime createdAt
) {
}
