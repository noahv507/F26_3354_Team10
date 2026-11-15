package edu.utd.se3354.team10.rhrs.service;

import edu.utd.se3354.team10.rhrs.model.*;
import edu.utd.se3354.team10.rhrs.repository.InMemoryHazardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HazardService (Road Hazard Reporting System, Team 10).
 * The repository is an in-memory fake and the notification service is a recording stub,
 * so HazardService is tested in isolation from the database and push provider.
 */
@DisplayName("HazardService")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class HazardServiceTest {

    /** Test double that records calls instead of sending real push notifications. */
    static class RecordingNotificationService implements NotificationService {
        final List<HazardReport> nearby = new ArrayList<>();
        final List<ConstructionSite> cleared = new ArrayList<>();
        @Override public void notifyNearby(HazardReport report) { nearby.add(report); }
        @Override public void notifyCleared(ConstructionSite site) { cleared.add(site); }
    }

    // Two points ~10 m apart and one ~1 km away, all in Richardson, TX
    static final Location CAMPBELL_RD = new Location(32.98600, -96.75000, "800 W Campbell Rd, Richardson, TX");
    static final Location TEN_METERS_AWAY = new Location(32.98609, -96.75000, "Near 800 W Campbell Rd");
    static final Location ONE_KM_AWAY = new Location(32.99500, -96.75000, "Renner Rd, Richardson, TX");

    InMemoryHazardRepository repository;
    RecordingNotificationService notifications;
    HazardService service;
    Driver driver;
    CityWorker worker;

    @BeforeEach
    void setUp() {
        repository = new InMemoryHazardRepository();
        notifications = new RecordingNotificationService();
        service = new HazardService(repository, new RoleBasedAuthService(), notifications);
        driver = new Driver("D1", "Dana Driver", "dana@example.com");
        worker = new CityWorker("W1", "Casey Worker", "casey@richardson.gov", "EMP-204");
    }

    static Pothole pothole(Location at, Photo photo) {
        return new Pothole("Deep pothole in right lane", at, photo, Severity.HIGH, LocalDateTime.now());
    }

    static Photo photoOfSize(long bytes) {
        return new Photo("P1", "https://cdn.example.com/p1.jpg", bytes);
    }

    @Nested
    @Order(1)
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("submitReport")
    class SubmitReport {

        @Test
        @DisplayName("TC-01: valid report is saved, given an ID, and nearby drivers are alerted")
        void validReportIsSaved() {
            String id = service.submitReport(pothole(CAMPBELL_RD, photoOfSize(2_000_000)), driver);

            assertEquals("HZ-00001", id);
            assertEquals(1, repository.count());
            assertEquals(HazardStatus.REPORTED, repository.findById(id).orElseThrow().getStatus());
            assertEquals(1, notifications.nearby.size());
        }

        @Test
        @DisplayName("TC-02: report with a blank description is rejected")
        void blankDescriptionRejected() {
            Pothole p = new Pothole("   ", CAMPBELL_RD, null, Severity.LOW, LocalDateTime.now());

            ValidationException ex = assertThrows(ValidationException.class, () -> service.submitReport(p, driver));
            assertEquals("Description is required", ex.getMessage());
            assertEquals(0, repository.count());
            assertTrue(notifications.nearby.isEmpty());
        }

        @Test
        @DisplayName("TC-03: report with an out-of-range location is rejected")
        void invalidLocationRejected() {
            Location bad = new Location(123.0, -96.75, "Nowhere");

            assertThrows(ValidationException.class, () -> service.submitReport(pothole(bad, null), driver));
            assertEquals(0, repository.count());
        }

        @Test
        @DisplayName("TC-04: photo of exactly 5 MB is accepted (boundary)")
        void photoAtLimitAccepted() {
            assertDoesNotThrow(() -> service.submitReport(pothole(CAMPBELL_RD, photoOfSize(Photo.MAX_SIZE_BYTES)), driver));
        }

        @Test
        @DisplayName("TC-05: photo one byte over 5 MB is rejected (boundary)")
        void photoOverLimitRejected() {
            Pothole p = pothole(CAMPBELL_RD, photoOfSize(Photo.MAX_SIZE_BYTES + 1));

            ValidationException ex = assertThrows(ValidationException.class, () -> service.submitReport(p, driver));
            assertEquals("Photo must be 5 MB or smaller", ex.getMessage());
        }

        @Test
        @DisplayName("TC-06: same hazard type within 50 m of an open report is rejected as a duplicate")
        void duplicateRejected() {
            String firstId = service.submitReport(pothole(CAMPBELL_RD, null), driver);

            DuplicateReportException ex = assertThrows(DuplicateReportException.class,
                    () -> service.submitReport(pothole(TEN_METERS_AWAY, null), driver));
            assertEquals(firstId, ex.getExistingReportId());
            assertEquals(1, repository.count());
        }

        @Test
        @DisplayName("TC-07: same hazard type more than 50 m away is not a duplicate")
        void farAwayNotDuplicate() {
            service.submitReport(pothole(CAMPBELL_RD, null), driver);

            assertDoesNotThrow(() -> service.submitReport(pothole(ONE_KM_AWAY, null), driver));
            assertEquals(2, repository.count());
        }

        @Test
        @DisplayName("TC-08: a new report is allowed where the earlier one is already RESOLVED")
        void resolvedReportDoesNotBlockNewOne() {
            String id = service.submitReport(pothole(CAMPBELL_RD, null), driver);
            service.updateStatus(id, HazardStatus.VERIFIED, worker, "Confirmed on site");
            service.updateStatus(id, HazardStatus.IN_PROGRESS, worker, "Crew assigned");
            service.updateStatus(id, HazardStatus.RESOLVED, worker, "Patched");

            assertDoesNotThrow(() -> service.submitReport(pothole(TEN_METERS_AWAY, null), driver));
        }

        @Test
        @DisplayName("TC-09: a signed-out user (null) cannot submit")
        void signedOutUserRejected() {
            assertThrows(UnauthorizedException.class, () -> service.submitReport(pothole(CAMPBELL_RD, null), null));
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("TC-10: city worker can move REPORTED -> VERIFIED and the change is logged")
        void workerCanVerify() {
            String id = service.submitReport(pothole(CAMPBELL_RD, null), driver);

            service.updateStatus(id, HazardStatus.VERIFIED, worker, "Confirmed on site");

            HazardReport r = repository.findById(id).orElseThrow();
            assertEquals(HazardStatus.VERIFIED, r.getStatus());
            assertEquals(1, r.getHistory().size());
            StatusUpdate u = r.getHistory().get(0);
            assertEquals(HazardStatus.REPORTED, u.oldStatus());
            assertEquals(HazardStatus.VERIFIED, u.newStatus());
            assertEquals("W1", u.workerId());
        }

        @Test
        @DisplayName("TC-11: a driver cannot change a report's status")
        void driverCannotUpdate() {
            String id = service.submitReport(pothole(CAMPBELL_RD, null), driver);

            assertThrows(UnauthorizedException.class,
                    () -> service.updateStatus(id, HazardStatus.VERIFIED, driver, "trying"));
            assertEquals(HazardStatus.REPORTED, repository.findById(id).orElseThrow().getStatus());
        }

        @Test
        @DisplayName("TC-12: skipping a status (REPORTED -> RESOLVED) is rejected")
        void cannotSkipStatus() {
            String id = service.submitReport(pothole(CAMPBELL_RD, null), driver);

            assertThrows(IllegalStateException.class,
                    () -> service.updateStatus(id, HazardStatus.RESOLVED, worker, "skip"));
        }

        @Test
        @DisplayName("TC-13: updating an unknown report ID fails")
        void unknownReportFails() {
            assertThrows(NoSuchElementException.class,
                    () -> service.updateStatus("HZ-99999", HazardStatus.VERIFIED, worker, ""));
        }

        @Test
        @DisplayName("TC-14: resolving a construction site notifies drivers it is cleared")
        void resolvingConstructionSiteNotifies() {
            ConstructionSite site = new ConstructionSite("Two lanes closed for utility work", CAMPBELL_RD, null,
                    LocalDate.now().plusDays(10), 2, LocalDateTime.now());
            String id = service.submitReport(site, driver);

            service.updateStatus(id, HazardStatus.VERIFIED, worker, "");
            service.updateStatus(id, HazardStatus.IN_PROGRESS, worker, "");
            assertTrue(notifications.cleared.isEmpty());

            service.updateStatus(id, HazardStatus.RESOLVED, worker, "Lanes reopened");
            assertEquals(1, notifications.cleared.size());
            assertFalse(site.isActive());
        }
    }
}
