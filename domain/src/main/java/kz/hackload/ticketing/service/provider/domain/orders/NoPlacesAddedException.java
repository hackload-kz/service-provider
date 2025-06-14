package kz.hackload.ticketing.service.provider.domain.orders;

public final class NoPlacesAddedException extends Exception
{
    private final OrderId orderId;

    public NoPlacesAddedException(final OrderId orderId)
    {
        super("Order %s does not have any place in it");
        this.orderId = orderId;
    }

    public OrderId orderId()
    {
        return orderId;
    }
}
