package com.vijayshri.jobtrackerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record DocumentRequest(

        @NotNull(message = "Application id is required")
        Long applicationId,

        @NotBlank(message = "File name is required")
        @Size(max = 200, message = "File name must be at most 200 characters")
        String fileName,

        @Size(max = 50, message = "File type must be at most 50 characters")
        String fileType,

        @NotBlank(message = "File URL is required")
        @URL(message = "File URL must be a valid URL")
        @Size(max = 500, message = "File URL must be at most 500 characters")
        String fileUrl
) {
}
