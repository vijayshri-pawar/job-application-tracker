package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.ApplicationStats;
import com.vijayshri.jobtrackerbackend.dto.DocumentResponse;
import com.vijayshri.jobtrackerbackend.dto.InterviewRoundResponse;
import com.vijayshri.jobtrackerbackend.dto.JobApplicationRequest;
import com.vijayshri.jobtrackerbackend.dto.JobApplicationResponse;
import com.vijayshri.jobtrackerbackend.exception.ResourceNotFoundException;
import com.vijayshri.jobtrackerbackend.model.Company;
import com.vijayshri.jobtrackerbackend.model.JobApplication;
import com.vijayshri.jobtrackerbackend.model.Status;
import com.vijayshri.jobtrackerbackend.model.User;
import com.vijayshri.jobtrackerbackend.repository.CompanyRepository;
import com.vijayshri.jobtrackerbackend.repository.DocumentRepository;
import com.vijayshri.jobtrackerbackend.repository.InterviewRoundRepository;
import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import com.vijayshri.jobtrackerbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Job applications of the logged-in user. Every method only sees that user's data. */
@Service
@RequiredArgsConstructor
@Transactional
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final DocumentRepository documentRepository;

    /**
     * @param status optional filter, e.g. "Interview" (null or blank = all)
     * @param search optional text matched against role and company name
     * @param sort   "newest" (default) or "oldest"
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getAll(Long userId, String status, String search, String sort) {
        Status statusFilter = Status.from(status);
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase(Locale.ROOT)) + "%";

        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort order = Sort.by(direction, "dateApplied").and(Sort.by(direction, "createdAt"));

        List<JobApplication> apps = statusFilter == null
                ? applicationRepository.search(userId, pattern, order)
                : applicationRepository.searchByStatus(userId, statusFilter, pattern, order);

        return apps.stream().map(JobApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getById(Long userId, Long id) {
        return JobApplicationResponse.from(findApplication(userId, id));
    }

    public JobApplicationResponse add(Long userId, JobApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        JobApplication app = new JobApplication();
        app.setUser(user);
        applyRequest(userId, app, request);
        return JobApplicationResponse.from(applicationRepository.save(app));
    }

    public JobApplicationResponse update(Long userId, Long id, JobApplicationRequest request) {
        JobApplication app = findApplication(userId, id);
        applyRequest(userId, app, request);
        return JobApplicationResponse.from(applicationRepository.save(app));
    }

    public void delete(Long userId, Long id) {
        applicationRepository.delete(findApplication(userId, id));
    }

    @Transactional(readOnly = true)
    public ApplicationStats getStats(Long userId) {
        long total = applicationRepository.countByUserId(userId);
        long applied = applicationRepository.countByUserIdAndStatus(userId, Status.APPLIED);
        long interviews = applicationRepository.countByUserIdAndStatus(userId, Status.INTERVIEW);
        long offers = applicationRepository.countByUserIdAndStatus(userId, Status.OFFER);
        long rejected = applicationRepository.countByUserIdAndStatus(userId, Status.REJECTED);

        long responded = interviews + offers + rejected;
        int responseRate = total == 0 ? 0 : (int) Math.round(responded * 100.0 / total);

        return new ApplicationStats(total, applied, interviews, offers, rejected, responseRate);
    }

    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> getInterviewRounds(Long userId, Long applicationId) {
        ensureApplicationExists(userId, applicationId);
        return interviewRoundRepository.findByJobApplicationIdOrderByRoundDateAsc(applicationId)
                .stream().map(InterviewRoundResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(Long userId, Long applicationId) {
        ensureApplicationExists(userId, applicationId);
        return documentRepository.findByJobApplicationId(applicationId)
                .stream().map(DocumentResponse::from).toList();
    }

    // ---------- helpers ----------

    private void applyRequest(Long userId, JobApplication app, JobApplicationRequest request) {
        Company company = companyRepository.findByIdAndUserId(request.companyId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));

        app.setCompany(company);
        app.setRole(request.role().trim());
        app.setDateApplied(request.dateApplied());
        app.setStatus(request.status());
        app.setJobLink(blankToNull(request.jobLink()));
        app.setLocation(blankToNull(request.location()));
    }

    /** Returns 404 both when the id does not exist and when it belongs to another user. */
    private JobApplication findApplication(Long userId, Long id) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application", id));
    }

    private void ensureApplicationExists(Long userId, Long id) {
        if (!applicationRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Job application", id);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
