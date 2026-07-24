package com.healthtrack.controller;

import com.healthtrack.dto.TaskDtos.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // Doctors and staff create/assign tasks. Enforced by role check below,
    // since patients shouldn't be assigning themselves work.
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.createTask(request, principal.getUser()));
    }

    // Unpaginated — kept for any existing caller that expects a bare array.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> listTasks(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.listTasksFor(principal.getUser()));
    }

    // Paginated + searchable. Frontend should move to this one.
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TaskResponse>> searchTasks(@AuthenticationPrincipal UserPrincipal principal,
                                                            @RequestParam(required = false) String q,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(taskService.searchTasksFor(principal.getUser(), q, pageable));
    }

    @PatchMapping("/{taskId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> updateProgress(@PathVariable Long taskId,
                                                          @Valid @RequestBody UpdateProgressRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.updateProgress(taskId, request, principal.getUser()));
    }

    @GetMapping("/{taskId}/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProgressNoteResponse>> getTimeline(@PathVariable Long taskId,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.getTimeline(taskId, principal.getUser()));
    }
}
