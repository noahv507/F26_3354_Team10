package edu.utd.se3354.team10.rhrs.service;

public class DuplicateReportException extends RuntimeException {
    private final String existingReportId;

    public DuplicateReportException(String existingReportId) {
        super("Hazard already reported as " + existingReportId);
        this.existingReportId = existingReportId;
    }

    public String getExistingReportId() { return existingReportId; }
}
