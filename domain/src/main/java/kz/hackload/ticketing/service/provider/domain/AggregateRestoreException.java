package kz.hackload.ticketing.service.provider.domain;

public final class AggregateRestoreException extends RuntimeException
{
    public AggregateRestoreException(final Throwable cause)
    {
        super(cause);
    }
}
