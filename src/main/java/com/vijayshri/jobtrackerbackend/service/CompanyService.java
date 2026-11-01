package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.CompanyRequest;
import com.vijayshri.jobtrackerbackend.dto.CompanyResponse;
import com.vijayshri.jobtrackerbackend.exception.ConflictException;
import com.vijayshri.jobtrackerbackend.exception.ResourceNotFoundException;
import com.vijayshri.jobtrackerbackend.model.Company;
import com.vijayshri.jobtrackerbackend.model.User;
import com.vijayshri.jobtrackerbackend.repository.CompanyRepository;
import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import com.vijayshri.jobtrackerbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Companies of the logged-in user. Each user has their own list. */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAll(Long userId) {
        return companyRepository.findByUserId(userId, Sort.by("name")).stream()
                .map(CompanyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long userId, Long id) {
        return CompanyResponse.from(findCompany(userId, id));
    }

    public CompanyResponse add(Long userId, CompanyRequest request) {
        String name = request.name().trim();
        if (companyRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("Company '" + name + "' already exists");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Company company = new Company();
        company.setUser(user);
        applyRequest(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public CompanyResponse update(Long userId, Long id, CompanyRequest request) {
        Company company = findCompany(userId, id);
        String name = request.name().trim();
        if (companyRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            throw new ConflictException("Company '" + name + "' already exists");
        }
        applyRequest(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public void delete(Long userId, Long id) {
        Company company = findCompany(userId, id);
        if (applicationRepository.existsByCompanyId(id)) {
            throw new ConflictException(
                    "Cannot delete '" + company.getName() + "' because it still has job applications");
        }
        companyRepository.delete(company);
    }

    private void applyRequest(Company company, CompanyRequest request) {
        company.setName(request.name().trim());
        company.setIndustry(blankToNull(request.industry()));
        company.setWebsite(blankToNull(request.website()));
    }

    private Company findCompany(Long userId, Long id) {
        return companyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
