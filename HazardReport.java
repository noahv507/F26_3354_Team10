package edu.utd.se3354.team10.rhrs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class for every hazard a driver can report. */
public abstract class HazardReport {
    private String reportId;
    private final HazardType hazardType;
    private final String description;
    private final Location location;
    private final Photo photo;           // optional (0..1)
    private final LocalDateTime reportDate;
    private HazardStatus status = HazardStatus.REPORTED;
    private final List<StatusUpdate> history = new ArrayList<>();

    protected HazardReport(HazardType hazardType, String description, Location location,
                           Photo photo, LocalDateTime reportDate) {
        this.hazardType = hazardType;
        this.description = description;
        this.location = location;
        this.photo = photo;
        this.reportDate = reportDate;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public HazardType getHazardType() { return hazardType; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public Photo getPhoto() { return photo; }
    public LocalDateTime getReportDate() { return reportDate; }
    public HazardStatus getStatus() { return status; }
    public List<StatusUpdate> getHistory() { return Collections.unmodifiableList(history); }

    /** Applies a status change and records it in the history. Caller must check permissions first. */
    public void updateStatus(HazardStatus newStatus, CityWorker worker, String note) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Cannot move report from " + status + " to " + newStatus);
        }
        history.add(new StatusUpdate("U" + (history.size() + 1), status, newStatus,
                LocalDateTime.now(), worker.getUserId(), note));
        status = newStatus;
    }

    public String getDetails() {
        return hazardType + " at " + location.getAddress() + " [" + status + "]: " + description;
    }
}
