package com.prachi.internshipproject.repository;

import com.prachi.internshipproject.entity.Issue;
import com.prachi.internshipproject.entity.IssuePriority;
import com.prachi.internshipproject.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByProjectId(Long projectId);
    List<Issue> findByProjectIdAndStatus(Long projectId, IssueStatus status);

    @Query("SELECT i FROM Issue i WHERE " +
           "(:projectId IS NULL OR i.project.id = :projectId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:priority IS NULL OR i.priority = :priority) AND " +
           "(:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Issue> searchAndFilterIssues(
            @Param("projectId") Long projectId,
            @Param("status") IssueStatus status,
            @Param("priority") IssuePriority priority,
            @Param("keyword") String keyword
    );
}