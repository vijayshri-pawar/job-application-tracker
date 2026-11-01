package com.vijayshri.jobtrackerbackend.dto;

import com.vijayshri.jobtrackerbackend.model.Document;

public record DocumentResponse(Long id, Long applicationId, String fileName, String fileType, String fileUrl) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getJobApplication().getId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileUrl()
        );
    }
}
