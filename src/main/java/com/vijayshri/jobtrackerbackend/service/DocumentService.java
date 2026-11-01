package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.DocumentRequest;
import com.vijayshri.jobtrackerbackend.dto.DocumentResponse;
import com.vijayshri.jobtrackerbackend.exception.ResourceNotFoundException;
import com.vijayshri.jobtrackerbackend.model.Document;
import com.vijayshri.jobtrackerbackend.model.JobApplication;
import com.vijayshri.jobtrackerbackend.repository.DocumentRepository;
import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final JobApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll(Long userId) {
        return documentRepository.findByJobApplicationUserId(userId).stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(Long userId, Long id) {
        return DocumentResponse.from(findDocument(userId, id));
    }

    public DocumentResponse add(Long userId, DocumentRequest request) {
        Document document = new Document();
        applyRequest(userId, document, request);
        return DocumentResponse.from(documentRepository.save(document));
    }

    public DocumentResponse update(Long userId, Long id, DocumentRequest request) {
        Document document = findDocument(userId, id);
        applyRequest(userId, document, request);
        return DocumentResponse.from(documentRepository.save(document));
    }

    public void delete(Long userId, Long id) {
        documentRepository.delete(findDocument(userId, id));
    }

    private void applyRequest(Long userId, Document document, DocumentRequest request) {
        JobApplication application = applicationRepository.findByIdAndUserId(request.applicationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application", request.applicationId()));

        document.setJobApplication(application);
        document.setFileName(request.fileName().trim());
        document.setFileType(request.fileType());
        document.setFileUrl(request.fileUrl().trim());
    }

    private Document findDocument(Long userId, Long id) {
        return documentRepository.findByIdAndJobApplicationUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }
}
