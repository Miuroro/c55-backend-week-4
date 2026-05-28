package com.weekfourassignment.AnalyticsAPI;

import com.weekfourassignment.AnalyticsAPI.dto.Request.AnalyticsRequest;
import com.weekfourassignment.AnalyticsAPI.dto.Response.AnalyticsResponse;
import com.weekfourassignment.AnalyticsAPI.dto.Summary.AnalyticsSummary;
import com.weekfourassignment.AnalyticsAPI.exceptions.ResourceNotFoundException;
import com.weekfourassignment.AnalyticsAPI.services.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AnalyticsApiApplicationTests {

	private AnalyticsService analyticsService;

	@BeforeEach
	void setUp() {
		analyticsService = new AnalyticsService();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void createRecordStoresAndReturnsGeneratedId() {
		AnalyticsResponse response = analyticsService.createRecord(request(
				"2026-05-28T10:15:30Z",
				"page_view",
				"web",
				"sess-1"
		));

		assertTrue(response.getId().startsWith("trace_"));
		assertEquals(OffsetDateTime.parse("2026-05-28T10:15:30Z"), response.getTimestamp());
		assertEquals("page_view", response.getEventType());
		assertEquals("web", response.getEventSource());
		assertEquals("sess-1", response.getSessionId());
		assertEquals(response, analyticsService.getRecordById(response.getId()));
	}

	@Test
	void getRecordsFiltersByTypeSourceSessionAndTimeRange() {
		analyticsService.createRecord(request("2026-05-28T08:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T09:00:00Z", "click", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T10:00:00Z", "page_view", "mobile", "sess-2"));

		List<AnalyticsResponse> filtered = analyticsService.getRecords(
				"page_view",
				"web",
				"sess-1",
				OffsetDateTime.parse("2026-05-28T07:30:00Z"),
				OffsetDateTime.parse("2026-05-28T08:30:00Z")
		);

		assertEquals(1, filtered.size());
		assertEquals("page_view", filtered.get(0).getEventType());
		assertEquals("web", filtered.get(0).getEventSource());
		assertEquals("sess-1", filtered.get(0).getSessionId());
	}

	@Test
	void getRecordByIdThrowsWhenMissing() {
		assertThrows(ResourceNotFoundException.class, () -> analyticsService.getRecordById("missing"));
	}

	@Test
	void replaceRecordFullyOverwritesExistingRecord() {
		AnalyticsResponse created = analyticsService.createRecord(request(
				"2026-05-28T10:15:30Z",
				"page_view",
				"web",
				"sess-1"
		));

		AnalyticsResponse updated = analyticsService.replaceRecord(created.getId(), request(
				"2026-05-28T12:00:00Z",
				"click",
				"mobile",
				"sess-2"
		));

		assertEquals(created.getId(), updated.getId());
		assertEquals(OffsetDateTime.parse("2026-05-28T12:00:00Z"), updated.getTimestamp());
		assertEquals("click", updated.getEventType());
		assertEquals("mobile", updated.getEventSource());
		assertEquals("sess-2", updated.getSessionId());
	}

	@Test
	void replaceRecordThrowsWhenRecordDoesNotExist() {
		assertThrows(ResourceNotFoundException.class, () -> analyticsService.replaceRecord(
				"missing",
				request("2026-05-28T12:00:00Z", "click", "mobile", "sess-2")
		));
	}

	@Test
	void deleteRecordRemovesRecordAndThrowsWhenMissing() {
		AnalyticsResponse created = analyticsService.createRecord(request(
				"2026-05-28T10:15:30Z",
				"page_view",
				"web",
				"sess-1"
		));

		analyticsService.deleteRecord(created.getId());

		assertThrows(ResourceNotFoundException.class, () -> analyticsService.getRecordById(created.getId()));
		assertThrows(ResourceNotFoundException.class, () -> analyticsService.deleteRecord(created.getId()));
	}

	@Test
	void getSummaryCountsTotalRecordsEventTypesAndUniqueSessions() {
		analyticsService.createRecord(request("2026-05-28T08:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T09:00:00Z", "page_view", "mobile", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T10:00:00Z", "click", "web", "sess-2"));

		AnalyticsSummary summary = analyticsService.getSummary();

		assertEquals(3L, summary.getTotalRecords());
		assertEquals(2L, summary.getTotalsByEventType().get("page_view"));
		assertEquals(1L, summary.getTotalsByEventType().get("click"));
		assertEquals(2L, summary.getUniqueSessions());
	}

	@Test
	void getSummaryReturnsZeroesWhenNoRecordsExist() {
		AnalyticsSummary summary = analyticsService.getSummary();

		assertEquals(0L, summary.getTotalRecords());
		assertTrue(summary.getTotalsByEventType().isEmpty());
		assertEquals(0L, summary.getUniqueSessions());
	}

	@Test
	void getSummaryIgnoresNullSessionIdsWhenCountingUniqueSessions() {
		analyticsService.createRecord(request("2026-05-28T08:00:00Z", "page_view", "web", null));
		analyticsService.createRecord(request("2026-05-28T09:00:00Z", "page_view", "mobile", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T10:00:00Z", "click", "web", null));

		AnalyticsSummary summary = analyticsService.getSummary();

		assertEquals(1L, summary.getUniqueSessions());
	}

	@Test
	void getRecordsRejectsInvalidTimeRange() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				analyticsService.getRecords(
						null,
						null,
						null,
						OffsetDateTime.parse("2026-05-28T12:00:00Z"),
						OffsetDateTime.parse("2026-05-28T10:00:00Z")
				)
		);

		assertTrue(exception.getMessage().contains("startTime"));
	}

	@Test
	void getRecordsReturnsAllWhenNoFiltersAreProvided() {
		analyticsService.createRecord(request("2026-05-28T08:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T09:00:00Z", "click", "web", "sess-2"));

		List<AnalyticsResponse> allRecords = analyticsService.getRecords(null, null, null, null, null);

		assertEquals(2, allRecords.size());
		assertFalse(allRecords.get(0).getId().isBlank());
		assertFalse(allRecords.get(1).getId().isBlank());
	}

	@Test
	void getRecordsIncludesValuesEqualToTimeBoundaries() {
		analyticsService.createRecord(request("2026-05-28T10:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T11:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T12:00:00Z", "page_view", "web", "sess-1"));

		List<AnalyticsResponse> filtered = analyticsService.getRecords(
				null,
				null,
				null,
				OffsetDateTime.parse("2026-05-28T11:00:00Z"),
				OffsetDateTime.parse("2026-05-28T12:00:00Z")
		);

		assertEquals(2, filtered.size());
		assertEquals(OffsetDateTime.parse("2026-05-28T12:00:00Z"), filtered.get(0).getTimestamp());
		assertEquals(OffsetDateTime.parse("2026-05-28T11:00:00Z"), filtered.get(1).getTimestamp());
	}

	@Test
	void getRecordsTrimsFilterValuesAndTreatsBlankAsNoFilter() {
		analyticsService.createRecord(request("2026-05-28T08:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T09:00:00Z", "click", "mobile", "sess-2"));

		List<AnalyticsResponse> trimmedMatch = analyticsService.getRecords(" page_view ", " web ", " sess-1 ", null, null);
		List<AnalyticsResponse> blankFilters = analyticsService.getRecords("   ", "\t", "\n", null, null);

		assertEquals(1, trimmedMatch.size());
		assertEquals("page_view", trimmedMatch.get(0).getEventType());
		assertEquals(2, blankFilters.size());
	}

	@Test
	void getRecordsOrdersByTimestampDescendingThenIdAscending() {
		analyticsService.createRecord(request("2026-05-28T10:00:00Z", "page_view", "web", "sess-1"));
		analyticsService.createRecord(request("2026-05-28T10:00:00Z", "click", "web", "sess-2"));
		analyticsService.createRecord(request("2026-05-28T09:00:00Z", "purchase", "mobile", "sess-3"));

		List<AnalyticsResponse> allRecords = analyticsService.getRecords(null, null, null, null, null);

		assertEquals(3, allRecords.size());
		assertEquals(OffsetDateTime.parse("2026-05-28T10:00:00Z"), allRecords.get(0).getTimestamp());
		assertEquals(OffsetDateTime.parse("2026-05-28T10:00:00Z"), allRecords.get(1).getTimestamp());
		assertTrue(allRecords.get(0).getId().compareTo(allRecords.get(1).getId()) < 0);
		assertEquals(OffsetDateTime.parse("2026-05-28T09:00:00Z"), allRecords.get(2).getTimestamp());
	}

	private AnalyticsRequest request(String timestamp, String eventType, String eventSource, String sessionId) {
		return new AnalyticsRequest(
				OffsetDateTime.parse(timestamp),
				eventType,
				eventSource,
				sessionId
		);
	}

}
