package kz.hackload.ticketing.service.provider.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StartedOrderDto(
        @JsonProperty("order_id")
        String orderId)
{
}
