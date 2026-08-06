package com.liftsync.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
public abstract class FullAudited extends CreationAudited {

    @UpdateTimestamp
    @Column(name = "last_modification", nullable = false)
    private Instant lastModification;

    public Instant getLastModification() {
        return lastModification;
    }
}
