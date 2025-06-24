package kz.hackload.ticketing.service.provider.domain.orders;

public record OrderConfirmedEvent() implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_confirmed_event";
    }
}
