package com.winter.fund.infrastructure.marketdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FundNavReadyBatchMessage(
    String eventId,
    LocalDate navDate,
    List<String> fundCodes,
    int count,
    LocalDateTime publishedAt
) {
}
