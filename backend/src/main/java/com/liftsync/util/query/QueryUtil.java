package com.liftsync.util.query;

import com.liftsync.util.debug.Debug;

import java.util.Locale;
import java.util.Map;

/**
 * Utility class for tracking and reporting SQL queries executed by the application.
 * Prints the query. how many appear and the method origin that creates that query
 * Take in mind that if this query checking lives under a @Transaction, the queries will not be captured because Hibernate flush.
 */
public class QueryUtil {


    private static final String APP_PACKAGE = "com.liftsync";

    /** Longest statement printed in the report; anything beyond this is elided. */
    private static final int SQL_PRINT_LIMIT = 1000;

    /** How many distinct call sites are kept per statement. */
    private static final int MAX_ORIGINS_PER_STATEMENT = 3;

    private static final int REPORT_WIDTH = 100;

    private static final StackWalker WALKER = StackWalker.getInstance();

    /** Wired by {@code DebugConfigurer} from {@code EnvironmentUtil} — true only in development. */
    private static volatile boolean enabled = false;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Trace active on the current thread, or {@code null} when not tracing. Per-thread on purpose:
     * one HTTP request = one thread, so two concurrent requests never mix their counters. Work
     * handed to another thread (@Async, executors, parallel streams) is NOT captured.
     */
    private static final ThreadLocal<QueryTrace> TRACE = new ThreadLocal<>();



    /**
     * Starts counting the SQL statements executed on this thread. Pair it with {@link #stop()},
     * which prints the report:
     *
     * <pre>{@code
     * Debug.start("copy week");
     * blockService.copyWeek(blockId, weekId);
     * Debug.stop();
     * }</pre>
     *
     * Only records inside a transaction that is still open: statements Hibernate defers to the
     * commit-time flush land after {@code stop()} if the transaction outlives it, so trace inside
     * the transactional method (or around the whole controller call) when counting writes.
     */
    public static void start() {
        start(null);
    }

    /** {@link #start()} with a label printed in the report header. */
    public static void start(String label) {
        if (!enabled) {
            System.err.println("[Debug.start] ignored outside dev profile — remove this debug call");
            return;
        }

        if (TRACE.get() != null) {
            System.err.println("[Debug.start] a trace was already running on this thread — restarting it");
        }
        TRACE.set(new QueryTrace(label));
    }

    /**
     * Stops the trace started by {@link #start()} and prints how many statements ran, grouped by
     * statement so repeats stand out.
     *
     * @return the number of statements executed while tracing (0 if no trace was running)
     */
    public static int stop() {
        QueryTrace trace = TRACE.get();
        if (trace == null) {
            if (enabled) {
                System.err.println("[Debug.stop] no active trace on this thread — call Debug.start() first");
            }
            return 0;
        }

        TRACE.remove();
        System.out.println(report(trace));
        return trace.total;
    }

    /** Statements recorded so far without stopping the trace. */
    public static int queryCount() {
        QueryTrace trace = TRACE.get();
        return trace == null ? 0 : trace.total;
    }

    /** Whether this thread is currently tracing. Used by the Hibernate inspector to stay cheap. */
    public static boolean isTracing() {
        return TRACE.get() != null;
    }

    /**
     * Records one statement. Called by the Hibernate {@code StatementInspector} wired in
     * {@code DebugConfigurer} for every SQL sent to the database; a no-op unless tracing.
     */
    public static void recordQuery(String sql) {
        QueryTrace trace = TRACE.get();
        if (trace == null || sql == null) {
            return;
        }

        trace.total++;
        QueryStat stat = trace.byStatement.computeIfAbsent(normalizeSql(sql), key -> new QueryStat());
        stat.count++;

        String origin = callerFrame();
        if (origin != null && stat.origins.size() < MAX_ORIGINS_PER_STATEMENT) {
            stat.origins.add(origin);
        }
    }

    /** Collapses Hibernate's whitespace so identical statements group together and print on one line. */
    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * The first application frame below the JPA/proxy machinery — i.e. the line that triggered the
     * query. Returns {@code null} when the query comes from outside application code (schema
     * validation, Flyway, connection setup).
     */
    private static String callerFrame() {
        return WALKER.walk(frames -> frames
                .filter(frame -> frame.getClassName().startsWith(APP_PACKAGE))
                .filter(frame -> !frame.getClassName().startsWith(Debug.class.getName()))
                .filter(frame -> !frame.getClassName().contains("DebugConfigurer"))
                .map(frame -> {
                    String className = frame.getClassName();
                    // Drop the CGLIB proxy suffix Spring adds to @Transactional beans.
                    int proxyMarker = className.indexOf("$$");
                    if (proxyMarker > 0) {
                        className = className.substring(0, proxyMarker);
                    }
                    String simpleName = className.substring(className.lastIndexOf('.') + 1);
                    return simpleName + "." + frame.getMethodName()
                            + "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
                })
                .findFirst()
                .orElse(null));
    }

    private static String report(QueryTrace trace) {
        double elapsedMs = (System.nanoTime() - trace.startedAtNanos) / 1_000_000d;
        String divider = "═".repeat(REPORT_WIDTH);

        StringBuilder sb = new StringBuilder("\n").append(divider).append("\n");
        sb.append(String.format(Locale.ROOT, " [Debug] %d queries (%d unique) in %.1f ms",
                trace.total, trace.byStatement.size(), elapsedMs));
        if (trace.label != null && !trace.label.isBlank()) {
            sb.append(" · ").append(trace.label);
        }
        sb.append("\n").append(divider).append("\n");

        if (trace.byStatement.isEmpty()) {
            sb.append(" no queries executed\n").append(divider);
            return sb.toString();
        }

        int index = 0;
        int repeated = 0;
        int repeatedExecutions = 0;
        for (Map.Entry<String, QueryStat> entry : trace.byStatement.entrySet()) {
            QueryStat stat = entry.getValue();

            if (stat.count > 1) {
                repeated++;
                repeatedExecutions += stat.count;
            }

            sb.append(String.format(" %2d. %s%s%n",
                    ++index,
                    stat.count > 1 ? "x" + stat.count + "  " : "     ",
                    truncate(entry.getKey())));

            for (String origin : stat.origins) {
                sb.append("     ↳ ").append(origin).append("\n");
            }
        }

        sb.append(divider).append("\n");
        if (repeated > 0) {
            sb.append(String.format(" ⚠ %d statement(s) repeated — %d of %d executions. Possible N+1.%n",
                    repeated, repeatedExecutions, trace.total));
            sb.append(divider);
        } else {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String truncate(String sql) {
        if (sql.length() <= SQL_PRINT_LIMIT) {
            return sql;
        }
        return sql.substring(0, SQL_PRINT_LIMIT) + "… (+" + (sql.length() - SQL_PRINT_LIMIT) + " chars)";
    }
}
