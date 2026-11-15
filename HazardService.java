package edu.utd.se3354.team10.rhrs.service;

import edu.utd.se3354.team10.rhrs.model.*;
import edu.utd.se3354.team10.rhrs.repository.HazardRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit under test. Server-side business logic for the Road Hazard Reporting System:
 * validates new reports, rejects duplicates, stores them, triggers alerts, and lets
 * only city workers move a report through its status lifecycle.
 */
public class HazardService {
    /** Reports of the same type closer than this to an open report are treated as duplicates. */
    public static final double DUPLICATE_RADIUS_METERS = 50.0;
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private final HazardRepository repository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public HazardService(HazardRepository repository, AuthService authService,
                         NotificationService notificationService) {
        this.repository = repository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    /** Validates, de-duplicates, saves and announces a report. Returns the new report ID. */
    public String submitReport(HazardReport report, User submitter) {
        if (!authService.authorize(submitter, "SUBMIT_REPORT")) {
            throw new UnauthorizedException("User is not signed in");
        }
        validate(report);
        HazardReport existing = findDuplicate(report);
        if (existing != null) {
            throw new DuplicateReportException(existing.getReportId());
        }
        report.setReportId(String.format("HZ-%05d", nextId.getAndIncrement()));
        repository.save(report);
        notificationService.notifyNearby(report);
        return report.getReportId();
    }

    /** Moves a report to its next status. Only city workers may do this. */
    public void updateStatus(String reportId, HazardStatus newStatus, User user, String note) {
        if (!authService.authorize(user, "UPDATE_STATUS") || !(user instanceof CityWorker worker)) {
            throw new UnauthorizedException("Only city workers can update report status");
        }
        HazardReport report = repository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("No report with ID " + reportId));
        report.updateStatus(newStatus, worker, note);
        repository.update(report);
        if (report instanceof ConstructionSite site && newStatus == HazardStatus.RESOLVED) {
            notificationService.notifyCleared(site);
        }
    }

    public List<HazardReport> getHazards(Location center, double radiusMeters) {
        return repository.findInArea(center, radiusMeters);
    }

    void validate(HazardReport report) {
        if (report == null) throw new ValidationException("Report is required");
        String d = report.getDescription();
        if (d == null || d.isBlank()) throw new ValidationException("Description is required");
        if (d.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description exceeds " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (report.getLocation() == null || !report.getLocation().isValid()) {
            throw new ValidationException("A valid location is required");
        }
        if (report.getPhoto() != null && !report.getPhoto().isValidSize()) {
            throw new ValidationException("Photo must be 5 MB or smaller");
        }
    }

    HazardReport findDuplicate(HazardReport report) {
        return repository.findInArea(report.getLocation(), DUPLICATE_RADIUS_METERS).stream()
                .filter(r -> r.getHazardType() == report.getHazardType())
                .filter(r -> r.getStatus() != HazardStatus.RESOLVED)
                .findFirst()
                .orElse(null);
    }
}
