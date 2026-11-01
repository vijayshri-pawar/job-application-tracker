package com.vijayshri.jobtrackerbackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "interview_round")
@Getter
@Setter
@NoArgsConstructor
public class InterviewRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String roundName;

    private LocalDate roundDate;

    @Column(length = 50)
    private String result;

    @Column(length = 1000)
    private String failureReason;

    @Column(length = 1000)
    private String mistakes;

    @Column(length = 1000)
    private String improvements;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication jobApplication;
}
