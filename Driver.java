package edu.utd.se3354.team10.rhrs.model;

public class Driver extends User {
    public Driver(String userId, String name, String email) { super(userId, name, email); }
    @Override public String getRole() { return "DRIVER"; }
}
