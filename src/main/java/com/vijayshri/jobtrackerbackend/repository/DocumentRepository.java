package com.vijayshri.jobtrackerbackend.repository;

import com.vijayshri.jobtrackerbackend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByJobApplicationId(Long applicationId);

    List<Document> findByJobApplicationUserId(Long userId);

    Optional<Document> findByIdAndJobApplicationUserId(Long id, Long userId);
}
