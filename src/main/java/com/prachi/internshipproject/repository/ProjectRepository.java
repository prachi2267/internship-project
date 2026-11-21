package com.prachi.internshipproject.repository;

import com.prachi.internshipproject.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}