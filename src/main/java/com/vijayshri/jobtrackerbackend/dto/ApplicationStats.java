package com.vijayshri.jobtrackerbackend.dto;

/**
 * Numbers for the dashboard cards.
 * responseRate = % of applications that got any reply (Interview, Offer or Rejected).
 */
public record ApplicationStats(
        long total,
        long applied,
        long interviews,
        long offers,
        long rejected,
        int responseRate
) {
}
