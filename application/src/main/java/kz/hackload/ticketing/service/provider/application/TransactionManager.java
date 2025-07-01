package kz.hackload.ticketing.service.provider.application;

import java.util.concurrent.Callable;

public interface TransactionManager
{
    void executeInTransaction(final Runnable runnable);

    <T> T executeInTransaction(final Callable<T> callable);
}
