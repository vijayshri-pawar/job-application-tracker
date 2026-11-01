package com.vijayshri.jobtrackerbackend.config;

import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once at startup: upgrades status values saved by the old version
 * ("Applied") to the new format ("APPLIED"), so filters and stats count them.
 * Safe to run every time; it does nothing when there is nothing to convert.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyDataMigration implements ApplicationRunner {

    private final JobApplicationRepository applicationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = applicationRepository.normalizeLegacyStatuses();
        if (updated > 0) {
            log.info("Converted {} job application status values to the new format", updated);
        }
    }
}
