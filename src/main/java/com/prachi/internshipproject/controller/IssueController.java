package com.prachi.internshipproject.controller;

import com.prachi.internshipproject.entity.*;
import com.prachi.internshipproject.repository.CommentRepository;
import com.prachi.internshipproject.repository.IssueRepository;
import com.prachi.internshipproject.repository.ProjectRepository;
import com.prachi.internshipproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IssueController {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    // Naya Bug/Issue create karne ke liye
    @PostMapping
    public ResponseEntity<Issue> createIssue(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody Map<String, Object> request) {

        Long projectId = Long.valueOf(request.get("projectId").toString());
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Issue.IssueBuilder builder = Issue.builder()
                .title((String) request.get("title"))
                .description((String) request.get("description"))
                .status(IssueStatus.TO_DO)
                .priority(IssuePriority.valueOf(
                        request.getOrDefault("priority", "MEDIUM").toString().toUpperCase()))
                .issueType(request.getOrDefault("issueType", "BUG").toString().toUpperCase())
                .project(project);

        if (request.get("assigneeId") != null && !request.get("assigneeId").toString().isEmpty()) {
            Long assigneeId = Long.valueOf(request.get("assigneeId").toString());
            builder.assignee(userRepository.findById(assigneeId).orElse(null));
        }

        Long reporterId = null;
        if (request.get("reporterId") != null && !request.get("reporterId").toString().isEmpty()) {
            reporterId = Long.valueOf(request.get("reporterId").toString());
        } else if (headerUserId != null) {
            reporterId = headerUserId;
        }

        if (reporterId != null) {
            builder.reporter(userRepository.findById(reporterId).orElse(null));
        }

        return ResponseEntity.ok(issueRepository.save(builder.build()));
    }

    // Kisi project ke saare issues dekhne ke liye
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Issue>> getIssuesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueRepository.findByProjectId(projectId));
    }

    // Saare issues dekhne ke liye
    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        return ResponseEntity.ok(issueRepository.findAll());
    }

    // Issues ko keyword, status, priority ya project ke basis par search/filter karne ke liye
    @GetMapping("/search")
    public ResponseEntity<List<Issue>> searchIssues(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword) {

        IssueStatus parsedStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL"))
                ? IssueStatus.valueOf(status.toUpperCase())
                : null;

        IssuePriority parsedPriority = (priority != null && !priority.isBlank() && !priority.equalsIgnoreCase("ALL"))
                ? IssuePriority.valueOf(priority.toUpperCase())
                : null;

        String cleanedKeyword = (keyword != null && !keyword.isBlank())
                ? keyword.trim()
                : null;

        List<Issue> results = issueRepository.searchAndFilterIssues(
                projectId,
                parsedStatus,
                parsedPriority,
                cleanedKeyword
        );

        return ResponseEntity.ok(results);
    }

    // Issue edit karne ke liye (with role/ownership check)
    @PutMapping("/{issueId}")
    public ResponseEntity<?> updateIssue(
            @PathVariable Long issueId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestBody Map<String, Object> request) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        if (!hasIssuePermission(issue, userId, roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Only ADMIN, PROJECT_MANAGER, or assigned/reporting DEVELOPER can edit this issue."));
        }

        if (request.get("title") != null && !request.get("title").toString().isBlank()) {
            issue.setTitle(request.get("title").toString().trim());
        }
        if (request.get("description") != null) {
            issue.setDescription(request.get("description").toString());
        }
        if (request.get("priority") != null && !request.get("priority").toString().isBlank()) {
            issue.setPriority(IssuePriority.valueOf(request.get("priority").toString().toUpperCase()));
        }
        if (request.get("issueType") != null && !request.get("issueType").toString().isBlank()) {
            issue.setIssueType(request.get("issueType").toString().toUpperCase());
        }
        if (request.containsKey("assigneeId")) {
            Object assigneeObj = request.get("assigneeId");
            if (assigneeObj == null || assigneeObj.toString().isBlank()) {
                issue.setAssignee(null);
            } else {
                Long assigneeId = Long.valueOf(assigneeObj.toString());
                issue.setAssignee(userRepository.findById(assigneeId).orElse(null));
            }
        }

        return ResponseEntity.ok(issueRepository.save(issue));
    }

    // Issue ka status update karne ke liye (Kanban Board ke liye)
    @PatchMapping("/{issueId}/status")
    public ResponseEntity<Issue> updateIssueStatus(
            @PathVariable Long issueId,
            @RequestBody Map<String, String> request) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        issue.setStatus(IssueStatus.valueOf(request.get("status").toUpperCase()));
        return ResponseEntity.ok(issueRepository.save(issue));
    }

    // Issue delete karne ke liye (with role/ownership check)
    @DeleteMapping("/{issueId}")
    @Transactional
    public ResponseEntity<?> deleteIssue(
            @PathVariable Long issueId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        if (!hasIssuePermission(issue, userId, roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Only ADMIN, PROJECT_MANAGER, or assigned/reporting DEVELOPER can delete this issue."));
        }

        commentRepository.deleteAll(commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId));
        issueRepository.deleteById(issueId);
        return ResponseEntity.ok(Map.of("message", "Issue deleted"));
    }

    private boolean hasIssuePermission(Issue issue, Long userId, String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            return false;
        }

        String normalizedRole = roleHeader.trim().toUpperCase();
        if ("ADMIN".equals(normalizedRole) || "PROJECT_MANAGER".equals(normalizedRole)) {
            return true;
        }

        if ("DEVELOPER".equals(normalizedRole) && userId != null) {
            boolean isAssignee = issue.getAssignee() != null && userId.equals(issue.getAssignee().getId());
            boolean isReporter = issue.getReporter() != null && userId.equals(issue.getReporter().getId());
            return isAssignee || isReporter;
        }

        return false;
    }
}