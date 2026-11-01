package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.InterviewRoundRequest;
import com.vijayshri.jobtrackerbackend.dto.InterviewRoundResponse;
import com.vijayshri.jobtrackerbackend.exception.ResourceNotFoundException;
import com.vijayshri.jobtrackerbackend.model.InterviewRound;
import com.vijayshri.jobtrackerbackend.model.JobApplication;
import com.vijayshri.jobtrackerbackend.repository.InterviewRoundRepository;
import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewRoundService {

    private final InterviewRoundRepository roundRepository;
    private final JobApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> getAll(Long userId) {
        return roundRepository.findByJobApplicationUserId(userId).stream().map(InterviewRoundResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InterviewRoundResponse getById(Long userId, Long id) {
        return InterviewRoundResponse.from(findRound(userId, id));
    }

    public InterviewRoundResponse add(Long userId, InterviewRoundRequest request) {
        InterviewRound round = new InterviewRound();
        applyRequest(userId, round, request);
        return InterviewRoundResponse.from(roundRepository.save(round));
    }

    public InterviewRoundResponse update(Long userId, Long id, InterviewRoundRequest request) {
        InterviewRound round = findRound(userId, id);
        applyRequest(userId, round, request);
        return InterviewRoundResponse.from(roundRepository.save(round));
    }

    public void delete(Long userId, Long id) {
        roundRepository.delete(findRound(userId, id));
    }

    private void applyRequest(Long userId, InterviewRound round, InterviewRoundRequest request) {
        JobApplication application = applicationRepository.findByIdAndUserId(request.applicationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application", request.applicationId()));

        round.setJobApplication(application);
        round.setRoundName(request.roundName().trim());
        round.setRoundDate(request.roundDate());
        round.setResult(request.result());
        round.setFailureReason(request.failureReason());
        round.setMistakes(request.mistakes());
        round.setImprovements(request.improvements());
    }

    private InterviewRound findRound(Long userId, Long id) {
        return roundRepository.findByIdAndJobApplicationUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round", id));
    }
}
