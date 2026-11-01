package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.ApplicationStats;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InterviewRoundRepository interviewRoundRepository;
    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private JobApplicationService service;

    private JobApplicationRequest validRequest(Long companyId) {
        return new JobApplicationRequest(companyId, " Java Developer ", LocalDate.now(),
                Status.APPLIED, "", "Pune");
    }

    @Test
    void getById_whenMissingOrOwnedByAnotherUser_throwsNotFound() {
        when(applicationRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job application not found with id 99");
    }

    @Test
    void add_withCompanyOfAnotherUser_throwsNotFoundAndSavesNothing() {
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(companyRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(USER_ID, validRequest(5L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found with id 5");
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void add_setsOwnerTrimsRoleAndStoresBlankLinkAsNull() {
        User user = new User();
        user.setId(USER_ID);
        Company company = new Company();
        company.setId(1L);
        company.setName("Google");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(companyRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(company));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication saved = inv.getArgument(0);
            assertThat(saved.getUser()).isSameAs(user);
            return saved;
        });

        JobApplicationResponse response = service.add(USER_ID, validRequest(1L));

        assertThat(response.role()).isEqualTo("Java Developer");
        assertThat(response.company()).isEqualTo("Google");
        assertThat(response.jobLink()).isNull();
        assertThat(response.status()).isEqualTo(Status.APPLIED);
    }

    @Test
    void getStats_calculatesResponseRateForThisUser() {
        when(applicationRepository.countByUserId(USER_ID)).thenReturn(4L);
        when(applicationRepository.countByUserIdAndStatus(USER_ID, Status.APPLIED)).thenReturn(2L);
        when(applicationRepository.countByUserIdAndStatus(USER_ID, Status.INTERVIEW)).thenReturn(1L);
        when(applicationRepository.countByUserIdAndStatus(USER_ID, Status.OFFER)).thenReturn(1L);
        when(applicationRepository.countByUserIdAndStatus(USER_ID, Status.REJECTED)).thenReturn(0L);

        ApplicationStats stats = service.getStats(USER_ID);

        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.responseRate()).isEqualTo(50);
    }

    @Test
    void getStats_withNoApplications_returnsZeroRate() {
        when(applicationRepository.countByUserId(USER_ID)).thenReturn(0L);

        assertThat(service.getStats(USER_ID).responseRate()).isZero();
    }

    @Test
    void getAll_withUnknownStatus_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.getAll(USER_ID, "Hired", null, "newest"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_whenMissing_throwsNotFound() {
        when(applicationRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, 3L)).isInstanceOf(ResourceNotFoundException.class);
        verify(applicationRepository, never()).delete(any());
    }
}
