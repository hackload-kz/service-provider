package kz.hackload.ticketing.service.provider.domain.orders;

public final class OrderAlreadyCancelledException extends Exception
{
    private final OrderId orderId;

    public OrderAlreadyCancelledException(final OrderId orderId)
    {
        super("Order %s is already cancelled".formatted(orderId));
        this.orderId = orderId;
    }

    public OrderId orderId()
    {
        return orderId;
    }
}
