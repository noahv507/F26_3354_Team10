package edu.utd.se3354.team10.rhrs.service;

import edu.utd.se3354.team10.rhrs.model.User;

public interface AuthService {
    /** True if the user is allowed to perform the action (e.g. "UPDATE_STATUS"). */
    boolean authorize(User user, String action);
}
