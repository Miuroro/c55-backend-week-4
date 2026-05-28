package com.weekfourassignment.AnalyticsAPI.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

	private String id;
	private OffsetDateTime timestamp;
	private String eventType;
	private String eventSource;
	private String sessionId;
}
