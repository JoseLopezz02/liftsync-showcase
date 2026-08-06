package com.liftsync.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component("liftSyncTransactionManager")
public class TransactionManager {

    public void runAfterCommit(Runnable task) {
        if (task == null) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive() &&
                TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            task.run();
                        }
                    });
        } else {

            //run the job immediately if there is no active transaction or synchronization
            task.run();

        }
    }
}