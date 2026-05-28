package com.weekfourassignment.AnalyticsAPI.controllers;

import com.weekfourassignment.AnalyticsAPI.dto.Request.AnalyticsRequest;
import com.weekfourassignment.AnalyticsAPI.dto.Response.AnalyticsResponse;
import com.weekfourassignment.AnalyticsAPI.dto.Summary.AnalyticsSummary;
import com.weekfourassignment.AnalyticsAPI.services.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	@PostMapping
	public ResponseEntity<AnalyticsResponse> createRecord(@Valid @RequestBody AnalyticsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(analyticsService.createRecord(request));
	}

	@GetMapping
	public ResponseEntity<List<AnalyticsResponse>> getRecords(
			@RequestParam(required = false) String eventType,
			@RequestParam(required = false) String eventSource,
			@RequestParam(required = false) String sessionId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime
	) {
		return ResponseEntity.ok(analyticsService.getRecords(eventType, eventSource, sessionId, startTime, endTime));
	}

	@GetMapping("/{id}")
	public ResponseEntity<AnalyticsResponse> getRecordById(@PathVariable String id) {
		return ResponseEntity.ok(analyticsService.getRecordById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AnalyticsResponse> replaceRecord(
			@PathVariable String id,
			@Valid @RequestBody AnalyticsRequest request
	) {
		return ResponseEntity.ok(analyticsService.replaceRecord(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteRecord(@PathVariable String id) {
		analyticsService.deleteRecord(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/summary")
	public ResponseEntity<AnalyticsSummary> getSummary() {
		return ResponseEntity.ok(analyticsService.getSummary());
	}
}
