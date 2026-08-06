package com.liftsync.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.model.Enum.Role;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.liftsync.exception.ForbiddenException;
import com.liftsync.exception.UnauthorizedException;

@Component
public class UserOwnershipInterceptor implements HandlerInterceptor {
    private final MessageResolver messageResolver;

    private static final String UUID_RX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    public UserOwnershipInterceptor(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    private final PathPatternParser parser = new PathPatternParser();

    // Rules are evaluated top-down; place the most specific first.
    // - requireOwnership=true -> the {idVar} in the path MUST equal the caller's publicId.
    //                              Path variables here are constrained to the UUID shape, so
    //                              literal segments (e.g. "athletes", "pending") cannot match.
    // - requireOwnership=false -> only role gating; no id comparison.
    private final List<Rule> rules = List.of(

            // --- Coach pending: only the pending-coach role can hit its own pending
            //     endpoint. Identity comes from the JWT, so there is no id in the path. ---
            new Rule("/api/coaches/pending", null,
                    EnumSet.of(Role.COACH_PENDING), false),

            // --- Athlete personal info: athlete owns it; coach can read it (relationship
            //     check is handled by @RequireCoachingRelationship on the handler). This is
            //     the only athlete resource that still carries the athlete publicId in the
            //     path, because a coach reads it by the athlete's id. ---
            new Rule("/api/athletes/{athleteId:" + UUID_RX + "}/personalInfo", "athleteId",
                    EnumSet.of(Role.ATHLETE), true),
            new Rule("/api/athletes/{athleteId:" + UUID_RX + "}/personalInfo", "athleteId",
                    EnumSet.of(Role.COACH), false),

            // --- Any other athlete/admin resource that still carries a publicId in the
            //     path must match the caller's own publicId (defense in depth). ---
            new Rule("/api/athletes/{athleteId:" + UUID_RX + "}/**", "athleteId",
                    EnumSet.of(Role.ATHLETE), true),
            new Rule("/api/admin/{adminId:" + UUID_RX + "}/**", "adminId",
                    EnumSet.of(Role.ADMIN), true),

            // --- Role-only fallbacks. The caller id is sourced from the JWT, not the URL,
            //     so coach/athlete self-ids are no longer present in these paths. ---
            // Coach endpoints (e.g. /api/coaches/athletes/{athleteId}/...)
            new Rule("/api/coaches/**", null, EnumSet.of(Role.COACH), false),
            // Athlete endpoints (athlete-self via JWT; coach allowed on dual-role reads)
            new Rule("/api/athletes/**", null, EnumSet.of(Role.ATHLETE, Role.COACH), false),
            // Admin endpoints
            new Rule("/api/admin/**", null, EnumSet.of(Role.ADMIN), false));

    @Override
    public boolean preHandle(HttpServletRequest req,
                             @NonNull HttpServletResponse res,
                             @NonNull Object handler) {

        if ("OPTIONS".equalsIgnoreCase(req.getMethod()))
            return true;

        Long currentUserId = (Long) req.getAttribute("userId");
        Role currentRole = (Role) req.getAttribute("userRole");
        String currentPublicId = (String) req.getAttribute("userPublicId");

        if (currentUserId == null || currentRole == null || currentPublicId == null) {
            String msg = messageResolver.getMessage("missing.user.identity");
            throw new UnauthorizedException(msg);
        }

        String pathRequest = req.getRequestURI();

        boolean matchedAnyPattern = false;
        boolean matchedAllowedRole = false;

        for (Rule rule : rules) {

            PathPattern pattern = parser.parse(rule.pattern());
            PathPattern.PathMatchInfo match = pattern.matchAndExtract(PathContainer.parsePath(pathRequest));

            if (match == null)
                continue;

            matchedAnyPattern = true;

            //If current rule not matches try next one
            if (!rule.allowedRoles().contains(currentRole)) continue;

            matchedAllowedRole = true;

            Map<String, String> vars = match.getUriVariables();
            String idStr = vars.get(rule.idVar());


            if (rule.requireOwnership()) {
                if (idStr == null) continue; // not a self-id rule for this path; try next

                // Path captures are constrained to the UUID shape, so idStr is a publicId.
                if (!currentPublicId.equals(idStr)) {
                    throw new ForbiddenException("You are not authorized to access this user data!");
                }

            }

            return true; // rule matched and passed
        }

        // If some rule matched the path but none allowed the role, forbid.
        if (matchedAnyPattern && !matchedAllowedRole) {

            throw new ForbiddenException("Role '" + currentRole + "' is not allowed for this resource");

        }

        // No rule matched -> valid user on the request
        return true;
    }

    private record Rule(String pattern, String idVar, Set<Role> allowedRoles, boolean requireOwnership) {
    }
}
