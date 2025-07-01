package kz.hackload.ticketing.service.provider.application;

import java.util.concurrent.Callable;

public final class NoopTransactionManager implements TransactionManager
{
    @Override
    public void executeInTransaction(final Runnable runnable)
    {
        runnable.run();
    }

    @Override
    public <T> T executeInTransaction(final Callable<T> callable)
    {
        try
        {
            return callable.call();
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
