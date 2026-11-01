package com.vijayshri.jobtrackerbackend.repository;

import com.vijayshri.jobtrackerbackend.model.Company;
import com.vijayshri.jobtrackerbackend.model.JobApplication;
import com.vijayshri.jobtrackerbackend.model.Status;
import com.vijayshri.jobtrackerbackend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobApplicationRepositoryTest {

    @Autowired
    private JobApplicationRepository applicationRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;

    private final Sort newestFirst = Sort.by(Sort.Direction.DESC, "dateApplied");
    private User asha;
    private User ravi;

    @BeforeEach
    void setUp() {
        asha = saveUser("Asha", "asha@example.com");
        ravi = saveUser("Ravi", "ravi@example.com");

        Company google = saveCompany("Google", asha);
        Company infosys = saveCompany("Infosys", asha);
        Company tcs = saveCompany("TCS", ravi);

        saveApplication(asha, google, "Java Developer", Status.APPLIED, LocalDate.of(2026, 10, 1));
        saveApplication(asha, google, "Backend Engineer", Status.INTERVIEW, LocalDate.of(2026, 10, 5));
        saveApplication(asha, infosys, "System Engineer", Status.OFFER, LocalDate.of(2026, 10, 3));
        saveApplication(ravi, tcs, "Java Developer", Status.APPLIED, LocalDate.of(2026, 10, 2));
    }

    @Test
    void search_returnsOnlyThisUsersApplications() {
        List<JobApplication> result = applicationRepository.search(asha.getId(), "%java%", newestFirst);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompany().getName()).isEqualTo("Google");
    }

    @Test
    void search_matchesCompanyNameCaseInsensitive() {
        List<JobApplication> result = applicationRepository.search(asha.getId(), "%google%", newestFirst);

        assertThat(result).extracting(JobApplication::getRole)
                .containsExactly("Backend Engineer", "Java Developer");
    }

    @Test
    void searchByStatus_filtersByStatusAndUser() {
        List<JobApplication> result =
                applicationRepository.searchByStatus(asha.getId(), Status.INTERVIEW, "%%", newestFirst);

        assertThat(result).extracting(JobApplication::getRole).containsExactly("Backend Engineer");
    }

    @Test
    void countsArePerUser() {
        assertThat(applicationRepository.countByUserId(asha.getId())).isEqualTo(3);
        assertThat(applicationRepository.countByUserId(ravi.getId())).isEqualTo(1);
        assertThat(applicationRepository.countByUserIdAndStatus(asha.getId(), Status.OFFER)).isEqualTo(1);
        assertThat(applicationRepository.countByUserIdAndStatus(ravi.getId(), Status.OFFER)).isZero();
    }

    @Test
    void findByIdAndUserId_hidesOtherUsersApplications() {
        Long ravisAppId = applicationRepository.search(ravi.getId(), "%%", newestFirst).get(0).getId();

        assertThat(applicationRepository.findByIdAndUserId(ravisAppId, ravi.getId())).isPresent();
        assertThat(applicationRepository.findByIdAndUserId(ravisAppId, asha.getId())).isEmpty();
    }

    @Test
    void companiesArePerUser() {
        assertThat(companyRepository.findByUserId(asha.getId(), Sort.by("name")))
                .extracting(Company::getName).containsExactly("Google", "Infosys");
        assertThat(companyRepository.existsByUserIdAndNameIgnoreCase(ravi.getId(), "google")).isFalse();
    }

    private User saveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("hash");
        return userRepository.save(user);
    }

    private Company saveCompany(String name, User owner) {
        Company company = new Company();
        company.setName(name);
        company.setUser(owner);
        return companyRepository.save(company);
    }

    private void saveApplication(User owner, Company company, String role, Status status, LocalDate date) {
        JobApplication app = new JobApplication();
        app.setUser(owner);
        app.setCompany(company);
        app.setRole(role);
        app.setStatus(status);
        app.setDateApplied(date);
        applicationRepository.save(app);
    }
}
