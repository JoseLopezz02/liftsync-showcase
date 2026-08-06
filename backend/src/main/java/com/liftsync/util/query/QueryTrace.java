package com.liftsync.util.query;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QueryTrace {
    final String label;
    final long startedAtNanos = System.nanoTime();
    int total = 0;

    final Map<String, QueryStat> byStatement = new LinkedHashMap<>();

    QueryTrace(String label) {
        this.label = label;
    }
}
