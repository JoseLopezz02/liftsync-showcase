package com.liftsync.config.amazon;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3SignedUrlProperties {

    private final long avatarMinutes;
    private final long coachTitleMinutes;
    private final long coachingLogoMinutes;
    private final long questionnairePhotoMinutes;

    public S3SignedUrlProperties(
            @Value("${aws.s3.signed-url.avatar.expiration-minutes}") long avatarMinutes,
            @Value("${aws.s3.signed-url.coach-title.expiration-minutes}") long coachTitleMinutes,
            @Value("${aws.s3.signed-url.coaching-logo.expiration-minutes}") long coachingLogoMinutes,
            @Value("${aws.s3.signed-url.questionnaire-photo.expiration-minutes}") long questionnairePhotoMinutes
    ) {
        this.avatarMinutes = avatarMinutes;
        this.coachTitleMinutes = coachTitleMinutes;
        this.coachingLogoMinutes = coachingLogoMinutes;
        this.questionnairePhotoMinutes = questionnairePhotoMinutes;
    }


    public Duration avatarDuration() {
        return Duration.ofMinutes(avatarMinutes);
    }

    public Duration coachTitleDuration() {
        return Duration.ofMinutes(coachTitleMinutes);
    }

    public Duration coachingLogoDuration() {
        return Duration.ofMinutes(coachingLogoMinutes);
    }

    public Duration questionnairePhotoDuration() {
        return Duration.ofMinutes(questionnairePhotoMinutes);
    }
}
