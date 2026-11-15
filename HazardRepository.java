package edu.utd.se3354.team10.rhrs.repository;

import edu.utd.se3354.team10.rhrs.model.HazardReport;
import edu.utd.se3354.team10.rhrs.model.Location;
import java.util.List;
import java.util.Optional;

/** Persistence boundary for hazard reports (a database in production). */
public interface HazardRepository {
    void save(HazardReport report);
    Optional<HazardReport> findById(String id);
    List<HazardReport> findInArea(Location center, double radiusMeters);
    void update(HazardReport report);
}
