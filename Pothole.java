package edu.utd.se3354.team10.rhrs.model;

import java.time.LocalDateTime;

public class Pothole extends HazardReport {
    private final Severity severity;

    public Pothole(String description, Location location, Photo photo, Severity severity,
                   LocalDateTime reportDate) {
        super(HazardType.POTHOLE, description, location, photo, reportDate);
        this.severity = severity;
    }

    public Severity getSeverity() { return severity; }
}
