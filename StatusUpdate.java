package edu.utd.se3354.team10.rhrs.model;

import java.time.LocalDateTime;

/** One entry in a report's status history, recorded by a city worker. */
public record StatusUpdate(String updateId, HazardStatus oldStatus, HazardStatus newStatus,
                           LocalDateTime timestamp, String workerId, String note) { }
