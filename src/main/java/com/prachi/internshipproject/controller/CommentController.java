package com.prachi.internshipproject.controller;

import com.prachi.internshipproject.entity.Comment;
import com.prachi.internshipproject.entity.Issue;
import com.prachi.internshipproject.entity.User;
import com.prachi.internshipproject.repository.CommentRepository;
import com.prachi.internshipproject.repository.IssueRepository;
import com.prachi.internshipproject.repository.UserRepository;
import com.prachi.internshipproject.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues/{issueId}/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CommentController(CommentRepository commentRepository,
                             IssueRepository issueRepository,
                             UserRepository userRepository,
                             JwtUtil jwtUtil) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public List getCommentsByIssue(@PathVariable Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found");
        }
        List<Comment> list = commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
        List result = new ArrayList<>();
        for (Comment comment : list) {
            result.add(toCommentMap(comment));
        }
        return result;
    }

    @PostMapping
    public ResponseEntity addComment(
            @PathVariable Long issueId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {

        Object rawContent = payload == null ? null : payload.get("content");
        String content = rawContent == null ? "" : rawContent.toString().trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        User author = resolveAuthor(headerUserId, authHeader, authentication);
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user found");
        }

        Comment comment = new Comment(content, issue, author);
        Comment saved = commentRepository.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommentMap(saved));
    }

    private User resolveAuthor(Long headerUserId, String authHeader, Authentication authentication) {
    if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
        User authUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (authUser != null) {
            return authUser;
        }
    }

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7).trim();
        if (jwtUtil.validateToken(token)) {
            String email = jwtUtil.extractEmail(token);
            User tokenUser = userRepository.findByEmail(email).orElse(null);
            if (tokenUser != null) {
                return tokenUser;
            }
        }
    }

    if (headerUserId != null) {
        User headerUser = userRepository.findById(headerUserId).orElse(null);
        if (headerUser != null) {
            return headerUser;
        }
    }

    return userRepository.findAll().stream().findFirst().orElse(null);
}

    private Map<String, Object> toCommentMap(Comment c) {
        Map<String, Object> authorMap = new LinkedHashMap<>();
        if (c.getAuthor() != null) {
            authorMap.put("id", c.getAuthor().getId());
            authorMap.put("name", c.getAuthor().getName());
            authorMap.put("email", c.getAuthor().getEmail());
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("content", c.getContent());
        map.put("createdAt", c.getCreatedAt());
        map.put("author", authorMap);
        return map;
    }
}