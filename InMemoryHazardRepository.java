package edu.utd.se3354.team10.rhrs.repository;

import edu.utd.se3354.team10.rhrs.model.HazardReport;
import edu.utd.se3354.team10.rhrs.model.Location;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Simple in-memory repository used for local runs and unit tests. */
public class InMemoryHazardRepository implements HazardRepository {
    private final Map<String, HazardReport> store = new LinkedHashMap<>();

    @Override public void save(HazardReport report) { store.put(report.getReportId(), report); }
    @Override public Optional<HazardReport> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public void update(HazardReport report) { store.put(report.getReportId(), report); }

    @Override
    public List<HazardReport> findInArea(Location center, double radiusMeters) {
        return store.values().stream()
                .filter(r -> r.getLocation().distanceTo(center) <= radiusMeters)
                .toList();
    }

    public int count() { return store.size(); }
}
