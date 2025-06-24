package kz.hackload.ticketing.service.provider.domain.orders;

public record OrderSubmittedEvent() implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_submitted_event";
    }
}
