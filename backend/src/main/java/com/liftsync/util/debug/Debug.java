package com.liftsync.util.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.liftsync.exception.DebugException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public final class Debug {

    /**
     * Response header the frontend watches for to render the debug dump overlay.
     */
    public static final String DUMP_HEADER = "X-Debug-Dump";

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Whether dump-and-die is allowed to run. Wired by {@code DebugConfigurer} from
     * {@code EnvironmentUtil} — true only in development. In any other environment
     * {@link #dd}/{@link #d} become no-ops so a stray debug call can never leak data in production.
     */
    private static volatile boolean enabled = false;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private Debug() {/* no‐op */}

    /**
     * Converts an object to a readable string representation.
     * Tries Jackson first, then falls back to reflection-based dump.
     */
    private static String dump(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }

        // Handle collections by dumping each element
        if (obj instanceof Collection<?> collection) {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            int jump = 0;
            for (Object item : collection) {
                if (jump > 0) sb.append(",\n");
                sb.append("  ").append(dump(item).replace("\n", "\n  "));
                jump++;
            }
            sb.append("\n]");
            return sb.toString();
        }

        // Handle maps
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) sb.append(",\n");
                sb.append("  \"").append(entry.getKey()).append("\": ");
                sb.append(dump(entry.getValue()).replace("\n", "\n  "));
                i++;
            }
            sb.append("\n}");
            return sb.toString();
        }

        // Try Jackson first
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // Fall back to reflection-based dump
            return reflectionDump(obj);
        }
    }

    /**
     * Uses reflection to dump all fields of an object when Jackson fails.
     */
    private static String reflectionDump(Object obj) {
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append(clazz.getSimpleName()).append(" {\n");

        // Get all fields including inherited ones
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    sb.append("  ").append(field.getName()).append(": ");
                    if (value == null) {
                        sb.append("null");
                    } else if (value instanceof String) {
                        sb.append("\"").append(value).append("\"");
                    } else if (value instanceof Number || value instanceof Boolean) {
                        sb.append(value);
                    } else if (value instanceof Collection || value instanceof Map) {
                        sb.append(dump(value).replace("\n", "\n  "));
                    } else {
                        // For nested objects, try to get a simple representation
                        try {
                            sb.append(MAPPER.writeValueAsString(value).replace("\n", "\n  "));
                        } catch (JsonProcessingException ex) {
                            sb.append(value.getClass().getSimpleName()).append("@").append(Integer.toHexString(value.hashCode()));
                        }
                    }
                    sb.append("\n");
                } catch (IllegalAccessException ex) {
                    sb.append("  ").append(field.getName()).append(": <inaccessible>\n");
                }
            }
            current = current.getSuperclass();
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Dump & die:
     * - prints each thing to stdout/logs
     * - throws DebugException carrying the dump
     */
    public static void dd(Object... things) {
        if (!enabled) {
            // Tripwire: a debug call survived into a non-dev profile. Never leak the dump
            // over HTTP — just warn loudly so it gets noticed and removed.
            System.err.println("[Debug.dd] ignored outside dev profile — remove this debug call");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Object t : things) {
            String dumped = dump(t);
            System.out.println(dumped);
            sb.append(dumped).append("\n");
        }

        // throw to unwind out of your controller
        throw new DebugException(sb.toString());
    }

    /**
     * Dump:
     * - prints each thing to stdout/logs
     */
    public static void d(Object... things) {
        if (!enabled) {
            System.err.println("[Debug.d] ignored outside dev profile — remove this debug call");
            return;
        }

        for (Object t : things) {
            String dumped = dump(t);
            System.out.println(dumped);

        }
    }


}