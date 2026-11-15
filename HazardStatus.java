package edu.utd.se3354.team10.rhrs.model;

/** Lifecycle of a hazard report. A report may only move forward through these states. */
public enum HazardStatus {
    REPORTED, VERIFIED, IN_PROGRESS, RESOLVED;

    /** True if moving from this status to {@code next} is a legal forward transition. */
    public boolean canTransitionTo(HazardStatus next) {
        return next != null && next.ordinal() == this.ordinal() + 1;
    }
}
