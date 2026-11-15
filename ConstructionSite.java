package edu.utd.se3354.team10.rhrs.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ConstructionSite extends HazardReport {
    private final LocalDate expectedEndDate;
    private final int laneClosures;

    public ConstructionSite(String description, Location location, Photo photo,
                            LocalDate expectedEndDate, int laneClosures, LocalDateTime reportDate) {
        super(HazardType.CONSTRUCTION_SITE, description, location, photo, reportDate);
        this.expectedEndDate = expectedEndDate;
        this.laneClosures = laneClosures;
    }

    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public int getLaneClosures() { return laneClosures; }
    public boolean isActive() { return getStatus() != HazardStatus.RESOLVED; }
}
