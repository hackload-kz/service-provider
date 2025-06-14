package kz.hackload.ticketing.service.provider.domain.orders;

public final class OrderNotStartedException extends Exception
{
    private final OrderId orderId;

    public OrderNotStartedException(final OrderId orderId)
    {
        super("Order %s is not started".formatted(orderId));
        this.orderId = orderId;
    }

    public OrderId orderId()
    {
        return orderId;
    }
}
