package com.liftsync.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.model.user.User;
import com.liftsync.model.Enum.Role;
import com.liftsync.service.user.CoachService;
import com.liftsync.service.user.UserService;
import com.liftsync.validation.RequireCoachingRelationship;

import java.util.Map;
import com.liftsync.exception.BadRequestException;
import com.liftsync.exception.ForbiddenException;
import com.liftsync.exception.UnauthorizedException;

@Component
public class CoachingRelationshipInterceptor implements HandlerInterceptor {

    private final CoachService coachService;
    private final UserService userService;
    private final MessageResolver messageResolver;

    public CoachingRelationshipInterceptor(CoachService coachService,
                                           UserService userService,
                                           MessageResolver messageResolver) {
        this.coachService = coachService;
        this.userService = userService;
        this.messageResolver = messageResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull Object handler){

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        // Only act on annotated handler methods
        if (!(handler instanceof HandlerMethod hm)) return true;
        RequireCoachingRelationship annotation = hm.getMethodAnnotation(RequireCoachingRelationship.class);
        if (annotation == null) {
            annotation = hm.getBeanType().getAnnotation(RequireCoachingRelationship.class);
        }
        if (annotation == null) return true;

        Long currentUserId = (Long) req.getAttribute("userId");
        Role currentRole   = (Role) req.getAttribute("userRole");
        if (currentUserId == null || currentRole == null) {
            String msg = messageResolver.getMessage("missing.user.identity");
            throw new UnauthorizedException(msg);
        }

        // Admins bypass the coaching-relationship check
        if (currentRole == Role.ADMIN) return true;

        // Only coaches can pass through coach-scoped endpoints
        if (currentRole != Role.COACH) {
            String msg = messageResolver.getMessage("coach.or.admin.allowed");
            throw new ForbiddenException(msg);
        }

        Map<String, String> pathVars =
                (Map<String, String>) req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars == null) {
            String msg = messageResolver.getMessage("invalid.path.variable");
            throw new BadRequestException(msg);
        }

        String athletePublicId = pathVars.get(annotation.athleteIdParam());
        if (athletePublicId == null) {
            String msg = messageResolver.getMessage("invalid.path.variable");
            throw new BadRequestException(msg);
        }

        User athlete = userService.findUserByPublicId(athletePublicId);
        if (athlete == null) {
            String msg = messageResolver.getMessage("invalid.path.variable");
            throw new BadRequestException(msg);
        }

        // Verify coaching relationship is accepted (caller is the coach via JWT)
        boolean accepted = coachService.existsRelationCoachingAccepted(currentUserId, athlete.getId());
        if (!accepted) {
            String msg = messageResolver.getMessage("no.coach.of.athlete");
            throw new ForbiddenException(msg);
        }

        return true;
    }
}
