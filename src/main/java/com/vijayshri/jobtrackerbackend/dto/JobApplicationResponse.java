package com.vijayshri.jobtrackerbackend.dto;

import com.vijayshri.jobtrackerbackend.model.Company;
import com.vijayshri.jobtrackerbackend.model.JobApplication;
import com.vijayshri.jobtrackerbackend.model.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Data the API returns for a job application. "company" is the company name. */
public record JobApplicationResponse(
        Long id,
        Long companyId,
        String company,
        String role,
        LocalDate dateApplied,
        Status status,
        String jobLink,
        String location,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobApplicationResponse from(JobApplication app) {
        Company company = app.getCompany();
        return new JobApplicationResponse(
                app.getId(),
                company != null ? company.getId() : null,
                company != null ? company.getName() : null,
                app.getRole(),
                app.getDateApplied(),
                app.getStatus(),
                app.getJobLink(),
                app.getLocation(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
