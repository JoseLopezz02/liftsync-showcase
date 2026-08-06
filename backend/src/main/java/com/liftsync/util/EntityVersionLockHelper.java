package com.liftsync.util;

import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.exception.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Component;

/**
 * Helper class for concurrency control on aggregate roots whose children were modified.
 * It bumps the @Version of the root and flushes so concurrent writers are detected,
 * translating a lock failure into a ConflictException with a localized message.
 */
@Component
public class EntityVersionLockHelper {

    private final EntityManager entityManager;
    private final MessageResolver messageResolver;

    public EntityVersionLockHelper(EntityManager entityManager, MessageResolver messageResolver) {
        this.entityManager = entityManager;
        this.messageResolver = messageResolver;
    }

    /**
     * Forces a @Version increment on the given managed entity and flushes the change,
     * even when the entity itself was not dirtied (only its children were).
     */
    public void forceIncrementVersion(Object entity, String conflictMessageKey) {
        try {
            entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new ConflictException(messageResolver.getMessage(conflictMessageKey));
        }
    }

    /**
     * Flushes the persistence context when the entity was already dirtied and no explicit
     * lock is needed, so the @Version bump happens as part of that update.
     */
    public void flushOrConflict(String conflictMessageKey) {
        try {
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new ConflictException(messageResolver.getMessage(conflictMessageKey));
        }
    }
}
