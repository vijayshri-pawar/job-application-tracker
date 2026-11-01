package com.vijayshri.jobtrackerbackend.repository;

import com.vijayshri.jobtrackerbackend.model.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {

    List<InterviewRound> findByJobApplicationIdOrderByRoundDateAsc(Long applicationId);

    List<InterviewRound> findByJobApplicationUserId(Long userId);

    Optional<InterviewRound> findByIdAndJobApplicationUserId(Long id, Long userId);
}
