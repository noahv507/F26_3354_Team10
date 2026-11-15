# Road Hazard Reporting System: HazardService Unit Tests

**Team 10 · CE/CS/SE 3354 Software Engineering · Fall 2026**

This repository holds the sample unit for Deliverable #2, Section 4.2. The unit is `HazardService`, the server-side class from our SDD class diagram. It:

- validates new hazard reports: description, location and the 5 MB photo limit
- rejects duplicate reports of the same hazard type within 50 m of an open report
- saves reports and alerts nearby drivers
- lets only city workers move a report through `REPORTED → VERIFIED → IN_PROGRESS → RESOLVED`
- alerts drivers when a construction site is cleared

The tests run with **JUnit 5 (Jupiter)**. The database and push-notification provider are replaced by an in-memory repository and a recording stub, so `HazardService` is tested on its own.

## Project layout

```
pom.xml                                   Maven build (JUnit 5 dependency)
src/main/java/edu/utd/se3354/team10/rhrs/
  model/        HazardReport, Pothole, ConstructionSite, Location, Photo, User, Driver, CityWorker, enums
  repository/   HazardRepository interface + InMemoryHazardRepository
  service/      HazardService (unit under test), AuthService, NotificationService, exceptions
src/test/java/edu/utd/se3354/team10/rhrs/service/
  HazardServiceTest.java                  14 JUnit test cases (TC-01 … TC-14)
junit-results.txt                         Output of the latest automated test run
```

## 1. Install dependencies

1. **Java JDK 17 or newer.** Check it with `java -version`.
   - Windows/macOS: install [Eclipse Temurin](https://adoptium.net/).
   - Ubuntu/Debian: `sudo apt install openjdk-17-jdk`
2. **Apache Maven 3.8 or newer.** Check it with `mvn -v`.
   - macOS: `brew install maven`
   - Windows: download it from [maven.apache.org](https://maven.apache.org/download.cgi), unzip it, and add its `bin` folder to `PATH`.
   - Ubuntu/Debian: `sudo apt install maven`

Maven downloads JUnit 5 by itself the first time you build. You don't need to install anything else.

## 2. Configure environment variables

The unit tests don't need any project-specific environment variables. They use an in-memory repository, so there is no database URL, API key or push-provider credential to set.

The only requirement is that Java and Maven are on your `PATH`:

| Variable | Needed? | Example |
|---|---|---|
| `JAVA_HOME` | Yes, if Maven can't find Java | macOS/Linux: `export JAVA_HOME=$(/usr/libexec/java_home)` or `/usr/lib/jvm/java-17-openjdk-amd64`<br>Windows: `setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17"` |
| `PATH` | Must include `$JAVA_HOME/bin` and Maven's `bin` folder | |

## 3. Run the project locally

From the repository root, run:

```bash
mvn test
```

The run should end with:

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Maven Surefire writes a report for each test class to `target/surefire-reports/`.

### Optional: run without Maven (JUnit Console Launcher)

Download `junit-platform-console-standalone` (1.10.x) from Maven Central, then run:

```bash
mkdir -p out/main out/test
javac -d out/main $(find src/main -name "*.java")
javac -d out/test -cp out/main:junit-platform-console-standalone.jar $(find src/test -name "*.java")
java -jar junit-platform-console-standalone.jar execute \
  -cp out/main:out/test \
  --select-class edu.utd.se3354.team10.rhrs.service.HazardServiceTest \
  --details=tree
```

On Windows, use `;` instead of `:` in the `-cp` paths.

## Test cases

| ID | Method | Input / setup | Expected result |
|---|---|---|---|
| TC-01 | submitReport | Valid pothole, 2 MB photo, signed-in driver | Saved as `HZ-00001`, status REPORTED, 1 nearby alert sent |
| TC-02 | submitReport | Description is blank (`"   "`) | `ValidationException` "Description is required"; nothing saved; no alert |
| TC-03 | submitReport | Latitude 123.0 (out of range) | `ValidationException`; nothing saved |
| TC-04 | submitReport | Photo exactly 5,242,880 bytes | Accepted (boundary) |
| TC-05 | submitReport | Photo 5,242,881 bytes | `ValidationException` "Photo must be 5 MB or smaller" |
| TC-06 | submitReport | Second pothole ~10 m from an open one | `DuplicateReportException` naming the first report; still 1 report stored |
| TC-07 | submitReport | Second pothole ~1 km away | Accepted; 2 reports stored |
| TC-08 | submitReport | New pothole ~10 m from a RESOLVED one | Accepted |
| TC-09 | submitReport | Submitter is `null` (signed out) | `UnauthorizedException` |
| TC-10 | updateStatus | City worker: REPORTED → VERIFIED | Status VERIFIED; 1 history entry with old/new status and worker ID |
| TC-11 | updateStatus | Driver tries REPORTED → VERIFIED | `UnauthorizedException`; status unchanged |
| TC-12 | updateStatus | City worker: REPORTED → RESOLVED (skips steps) | `IllegalStateException` |
| TC-13 | updateStatus | Unknown ID `HZ-99999` | `NoSuchElementException` |
| TC-14 | updateStatus | Construction site moved to RESOLVED | Exactly 1 "cleared" alert sent, only on RESOLVED; site no longer active |

Actual result for all 14 tests: **Pass**. See `junit-results.txt`.
