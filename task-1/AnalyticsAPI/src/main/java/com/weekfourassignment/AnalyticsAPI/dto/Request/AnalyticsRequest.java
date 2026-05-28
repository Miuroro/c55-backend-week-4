package com.weekfourassignment.AnalyticsAPI.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsRequest {

	@NotNull
	private OffsetDateTime timestamp;

	@NotBlank
	private String eventType;

	@NotBlank
	private String eventSource;

	@NotBlank
	private String sessionId;
}
