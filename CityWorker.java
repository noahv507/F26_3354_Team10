package edu.utd.se3354.team10.rhrs.model;

public class CityWorker extends User {
    private final String employeeId;

    public CityWorker(String userId, String name, String email, String employeeId) {
        super(userId, name, email);
        this.employeeId = employeeId;
    }

    public String getEmployeeId() { return employeeId; }
    @Override public String getRole() { return "CITY_WORKER"; }
}
