package com.liftsync.messaging.email;

import java.util.Set;

public enum EmailType {
    WELCOME_ATHLETE(
            Set.of("username", "fullName"), "email/welcome-athlete", "mail.welcomeAthlete.subject", "fullName"),
    WELCOME_COACH_PENDING(
            Set.of("fullName"), "email/welcome-coach-pending", "mail.welcomeCoachPending.subject", "fullName"),
    COACH_PENDING_APPROVED(
            Set.of("fullName"), "email/coach-pending-approved", "mail.coachPending.accepted.subject", "fullName"),
    COACH_PENDING_REJECTED(
            Set.of("fullName"), "email/coach-pending-declined", "mail.coachPending.rejected.subject", "fullName"),
    PASSWORD_RESET_SUCCESS(
            Set.of("fullName"), "email/password-reset-success", "mail.passwordReset.success.subject", "fullName"),
    VERIFICATION_CODE(
            Set.of("code"), "email/verification-code", "mail.verificationCode.subject", null),
    PASSWORD_RECOVERY(
            Set.of("recoveryLink"), "email/password-recovery", "mail.passwordRecovery.subject", null),
    COACH_INVITE(
            Set.of("coachName", "inviteLink", "token"), "email/coach-invite", "mail.coachInvite.subject", "coachName");


    private final Set<String> requiredVariables;
    private final String templateName;
    private final String subjectKey;
    private final String subjectArg;

    EmailType(Set<String> requiredVariables, String templateName, String subjectKey, String subjectArg) {
        this.requiredVariables = requiredVariables;
        this.templateName = templateName;
        this.subjectKey = subjectKey;
        this.subjectArg = subjectArg;
    }

    public Set<String> getRequiredVariables() {
        return this.requiredVariables;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public String getSubjectKey() {
        return this.subjectKey;
    }

    public String getSubjectArg() {
        return this.subjectArg;
    }
}
