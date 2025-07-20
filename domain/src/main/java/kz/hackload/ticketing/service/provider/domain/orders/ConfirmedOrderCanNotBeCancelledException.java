package kz.hackload.ticketing.service.provider.domain.orders;

public final class ConfirmedOrderCanNotBeCancelledException extends Exception
{
    private final OrderId orderId;

    public ConfirmedOrderCanNotBeCancelledException(final OrderId orderId)
    {
        super("Confirmed order %s can not be cancelled".formatted(orderId));
        this.orderId = orderId;
    }

    public OrderId orderId()
    {
        return orderId;
    }
}
