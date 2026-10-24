package com.campussync.dto;

import java.time.LocalDateTime;

public record NotificationPublicationResponse(Long id, String title, String category,
                                              int recipientCount, LocalDateTime createdAt) { }
