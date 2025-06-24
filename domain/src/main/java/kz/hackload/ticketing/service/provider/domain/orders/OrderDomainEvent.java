package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.DomainEvent;

public sealed interface OrderDomainEvent extends DomainEvent
        permits OrderCancelledEvent, OrderConfirmedEvent, OrderStartedEvent, OrderSubmittedEvent, PlaceAddedToOrderEvent, PlaceRemovedFromOrderEvent
{
}
