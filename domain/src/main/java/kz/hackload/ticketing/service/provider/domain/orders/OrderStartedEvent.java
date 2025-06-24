package kz.hackload.ticketing.service.provider.domain.orders;

public record OrderStartedEvent() implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_started_event";
    }
}
