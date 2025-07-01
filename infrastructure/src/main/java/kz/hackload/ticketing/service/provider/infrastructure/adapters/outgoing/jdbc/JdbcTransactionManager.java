package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import kz.hackload.ticketing.service.provider.application.TransactionManager;

public final class JdbcTransactionManager implements TransactionManager
{
    private final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    private final DataSource dataSource;

    public JdbcTransactionManager(final DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void executeInTransaction(final Runnable runnable)
    {
        try (final Connection connection = dataSource.getConnection())
        {
            connection.setAutoCommit(false);

            currentConnection.set(connection);
            runnable.run();

            connection.commit();
        }
        catch (final SQLException e)
        {
            final Connection connection = currentConnection.get();

            if (connection == null)
            {
                throw new RuntimeException(e);
            }

            try
            {
                connection.rollback();
            }
            catch (final SQLException rollbackException)
            {
                e.addSuppressed(rollbackException);
            }

            throw new RuntimeException(e);
        }
        finally
        {
            currentConnection.remove();
        }
    }

    @Override
    public <T> T executeInTransaction(final Callable<T> callable)
    {
        try (final Connection connection = dataSource.getConnection())
        {
            connection.setAutoCommit(false);

            currentConnection.set(connection);
            final T result = callable.call();

            connection.commit();

            return result;
        }
        catch (final Exception e)
        {
            final Connection connection = currentConnection.get();

            if (connection == null)
            {
                throw new RuntimeException(e);
            }

            try
            {
                connection.rollback();
            }
            catch (final SQLException rollbackException)
            {
                e.addSuppressed(rollbackException);
            }

            throw new RuntimeException(e);
        }
        finally
        {
            currentConnection.remove();
        }
    }

    public Connection currentConnection()
    {
        return currentConnection.get();
    }
}
