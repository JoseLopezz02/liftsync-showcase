package com.liftsync.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@MappedSuperclass
public abstract class CreationAudited {

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Instant creationDate;

    public Instant getCreationDate() {
        return creationDate;
    }
}
