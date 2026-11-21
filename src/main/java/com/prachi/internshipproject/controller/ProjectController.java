package com.prachi.internshipproject.controller;

import com.prachi.internshipproject.entity.Project;
import com.prachi.internshipproject.entity.User;
import com.prachi.internshipproject.repository.ProjectRepository;
import com.prachi.internshipproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Map<String, Object> request) {
        Project.ProjectBuilder builder = Project.builder()
                .name((String) request.get("name"))
                .projectKey(((String) request.get("projectKey")).toUpperCase())
                .description((String) request.get("description"));

        if (request.get("ownerId") != null) {
            Long ownerId = Long.valueOf(request.get("ownerId").toString());
            User owner = userRepository.findById(ownerId).orElse(null);
            builder.owner(owner);
        }

        return ResponseEntity.ok(projectRepository.save(builder.build()));
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }
}