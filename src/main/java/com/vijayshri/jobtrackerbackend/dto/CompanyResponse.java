package com.vijayshri.jobtrackerbackend.dto;

import com.vijayshri.jobtrackerbackend.model.Company;

public record CompanyResponse(Long id, String name, String industry, String website) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getIndustry(), company.getWebsite());
    }
}
