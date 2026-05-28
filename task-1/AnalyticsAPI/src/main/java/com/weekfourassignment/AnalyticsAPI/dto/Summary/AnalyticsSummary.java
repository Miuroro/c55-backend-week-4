package com.weekfourassignment.AnalyticsAPI.dto.Summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummary {

	private long totalRecords;
	private Map<String, Long> totalsByEventType;
	private long uniqueSessions;
}
