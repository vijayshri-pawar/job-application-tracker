package com.vijayshri.jobtrackerbackend.repository;

import com.vijayshri.jobtrackerbackend.model.JobApplication;
import com.vijayshri.jobtrackerbackend.model.Status;
import com.vijayshri.jobtrackerbackend.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /** One user's applications matching role or company name ("pattern" is lower-case and wrapped in %). */
    @Query("""
            select a from JobApplication a
            left join fetch a.company c
            where a.user.id = :userId
              and (lower(a.role) like :pattern or lower(c.name) like :pattern)
            """)
    List<JobApplication> search(@Param("userId") Long userId,
                                @Param("pattern") String pattern,
                                Sort sort);

    /** Same as {@link #search} but only for one status. */
    @Query("""
            select a from JobApplication a
            left join fetch a.company c
            where a.user.id = :userId
              and a.status = :status
              and (lower(a.role) like :pattern or lower(c.name) like :pattern)
            """)
    List<JobApplication> searchByStatus(@Param("userId") Long userId,
                                        @Param("status") Status status,
                                        @Param("pattern") String pattern,
                                        Sort sort);

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Status status);

    boolean existsByCompanyId(Long companyId);

    /**
     * The old version saved status as "Applied", "Interview", ... ;
     * this version stores the enum name ("APPLIED", "INTERVIEW", ...). Converts old rows once.
     */
    @Modifying
    @Transactional
    @Query(value = "update job_application set status = upper(status) where status <> upper(status)",
            nativeQuery = true)
    int normalizeLegacyStatuses();

    /**
     * Gives applications created before login existed (no owner, or owned by a user
     * without a password) to the given user. Used once, when the first account is registered.
     */
    @Modifying
    @Transactional
    @Query("""
            update JobApplication a set a.user = :user
            where a.user is null
               or a.user.id in (select u.id from User u where u.passwordHash is null)
            """)
    int assignUnownedTo(@Param("user") User user);
}
