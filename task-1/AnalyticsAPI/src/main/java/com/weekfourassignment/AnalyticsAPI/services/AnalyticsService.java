package com.weekfourassignment.AnalyticsAPI.services;

import com.weekfourassignment.AnalyticsAPI.dto.Request.AnalyticsRequest;
import com.weekfourassignment.AnalyticsAPI.dto.Response.AnalyticsResponse;
import com.weekfourassignment.AnalyticsAPI.dto.Summary.AnalyticsSummary;
import com.weekfourassignment.AnalyticsAPI.exceptions.ResourceNotFoundException;
import com.weekfourassignment.AnalyticsAPI.model.AnalyticsRecord;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

	private final Map<String, AnalyticsRecord> records = new ConcurrentHashMap<>();
	private final AtomicLong traceSequence = new AtomicLong(0L);

	public AnalyticsResponse createRecord(AnalyticsRequest request) {
		AnalyticsRecord record = toRecord(generateId(), request);
		records.put(record.getId(), record);
		return toResponse(record);
	}

	public List<AnalyticsResponse> getRecords(String eventType, String eventSource, String sessionId,
											  OffsetDateTime startTime, OffsetDateTime endTime) {
		validateTimeRange(startTime, endTime);

		return records.values().stream()
				.filter(record -> matchesTextFilter(record.getEventType(), eventType))
				.filter(record -> matchesTextFilter(record.getEventSource(), eventSource))
				.filter(record -> matchesTextFilter(record.getSessionId(), sessionId))
				.filter(record -> matchesStartTime(record.getTimestamp(), startTime))
				.filter(record -> matchesEndTime(record.getTimestamp(), endTime))
				.sorted(Comparator.comparing(AnalyticsRecord::getTimestamp).reversed()
						.thenComparing(AnalyticsRecord::getId))
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public AnalyticsResponse getRecordById(String id) {
		return toResponse(findRecord(id));
	}

	public AnalyticsResponse replaceRecord(String id, AnalyticsRequest request) {
		if (!records.containsKey(id)) {
			throw new ResourceNotFoundException("Analytics record not found with id: " + id);
		}

		AnalyticsRecord updatedRecord = toRecord(id, request);
		records.put(id, updatedRecord);
		return toResponse(updatedRecord);
	}

	public void deleteRecord(String id) {
		AnalyticsRecord removed = records.remove(id);
		if (removed == null) {
			throw new ResourceNotFoundException("Analytics record not found with id: " + id);
		}
	}

	public AnalyticsSummary getSummary() {
		Collection<AnalyticsRecord> currentRecords = records.values();

		Map<String, Long> totalsByEventType = currentRecords.stream()
				.collect(Collectors.groupingBy(
						AnalyticsRecord::getEventType,
						LinkedHashMap::new,
						Collectors.counting()));

		long uniqueSessions = currentRecords.stream()
				.map(AnalyticsRecord::getSessionId)
				.filter(Objects::nonNull)
				.distinct()
				.count();

		return new AnalyticsSummary(currentRecords.size(), totalsByEventType, uniqueSessions);
	}

	private AnalyticsRecord findRecord(String id) {
		AnalyticsRecord record = records.get(id);
		if (record == null) {
			throw new ResourceNotFoundException("Analytics record not found with id: " + id);
		}
		return record;
	}

	private AnalyticsRecord toRecord(String id, AnalyticsRequest request) {
		return new AnalyticsRecord(
				id,
				request.getTimestamp(),
				request.getEventType(),
				request.getEventSource(),
				request.getSessionId()
		);
	}

	private AnalyticsResponse toResponse(AnalyticsRecord record) {
		return new AnalyticsResponse(
				record.getId(),
				record.getTimestamp(),
				record.getEventType(),
				record.getEventSource(),
				record.getSessionId()
		);
	}

	private String generateId() {
		long sequence = traceSequence.incrementAndGet();
		return "trace_" + sequence + "_" + UUID.randomUUID().toString().replace("-", "");
	}

	private boolean matchesTextFilter(String value, String filter) {
		if (filter == null || filter.isBlank()) {
			return true;
		}
		return value != null && value.equals(filter.trim());
	}

	private boolean matchesStartTime(OffsetDateTime timestamp, OffsetDateTime startTime) {
		return startTime == null || !timestamp.isBefore(startTime);
	}

	private boolean matchesEndTime(OffsetDateTime timestamp, OffsetDateTime endTime) {
		return endTime == null || !timestamp.isAfter(endTime);
	}

	private void validateTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
		if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
			throw new IllegalArgumentException("startTime must be less than or equal to endTime");
		}
	}
}
