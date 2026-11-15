package edu.utd.se3354.team10.rhrs.service;

import edu.utd.se3354.team10.rhrs.model.ConstructionSite;
import edu.utd.se3354.team10.rhrs.model.HazardReport;

public interface NotificationService {
    /** Alert drivers within their alert radius of a newly reported hazard. */
    void notifyNearby(HazardReport report);
    /** Alert drivers that a construction site has been cleared. */
    void notifyCleared(ConstructionSite site);
}
