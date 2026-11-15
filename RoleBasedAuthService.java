package edu.utd.se3354.team10.rhrs.service;

import edu.utd.se3354.team10.rhrs.model.User;

/** Only city workers may change a report's status; anyone signed in may submit. */
public class RoleBasedAuthService implements AuthService {
    @Override
    public boolean authorize(User user, String action) {
        if (user == null) return false;
        return switch (action) {
            case "SUBMIT_REPORT" -> true;
            case "UPDATE_STATUS" -> "CITY_WORKER".equals(user.getRole());
            default -> false;
        };
    }
}
