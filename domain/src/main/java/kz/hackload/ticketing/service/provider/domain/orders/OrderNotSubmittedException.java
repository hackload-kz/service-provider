package kz.hackload.ticketing.service.provider.domain.orders;

public final class OrderNotSubmittedException extends Exception
{
    private final OrderId orderId;

    public OrderNotSubmittedException(final OrderId orderId)
    {
        super("Order %s is not submitted".formatted(orderId));
        this.orderId = orderId;
    }

    public OrderId orderId()
    {
        return orderId;
    }
}
