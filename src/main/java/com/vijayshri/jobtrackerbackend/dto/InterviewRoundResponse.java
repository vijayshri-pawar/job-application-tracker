package com.vijayshri.jobtrackerbackend.dto;

import com.vijayshri.jobtrackerbackend.model.InterviewRound;

import java.time.LocalDate;

public record InterviewRoundResponse(
        Long id,
        Long applicationId,
        String roundName,
        LocalDate roundDate,
        String result,
        String failureReason,
        String mistakes,
        String improvements
) {
    public static InterviewRoundResponse from(InterviewRound round) {
        return new InterviewRoundResponse(
                round.getId(),
                round.getJobApplication().getId(),
                round.getRoundName(),
                round.getRoundDate(),
                round.getResult(),
                round.getFailureReason(),
                round.getMistakes(),
                round.getImprovements()
        );
    }
}
