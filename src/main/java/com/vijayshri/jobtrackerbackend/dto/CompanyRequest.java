package com.vijayshri.jobtrackerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CompanyRequest(

        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must be at most 150 characters")
        String name,

        @Size(max = 100, message = "Industry must be at most 100 characters")
        String industry,

        @URL(message = "Website must be a valid URL")
        @Size(max = 300, message = "Website must be at most 300 characters")
        String website
) {
}
