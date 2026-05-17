package com.liftsync.cache;

/**
 * Utility class for generating cache keys for various cache entries.
 */
public final class CacheKeyUtils {

    private CacheKeyUtils() {
    }

    public static String athleteMetrics(Long athleteId, Long coachId) {
        if (athleteId == null || coachId == null) {
            return null;
        }

        return athleteId + ":" + coachId;
    }

}