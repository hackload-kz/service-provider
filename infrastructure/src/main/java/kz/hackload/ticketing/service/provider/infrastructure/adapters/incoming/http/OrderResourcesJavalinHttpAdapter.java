package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import java.util.UUID;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SubmitOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;

public class OrderResourcesJavalinHttpAdapter
{
    private final StartOrderUseCase startOrderUseCase;
    private final SubmitOrderUseCase submitOrderUseCase;

    public OrderResourcesJavalinHttpAdapter(final Javalin server,
                                            final StartOrderUseCase startOrderUseCase,
                                            final SubmitOrderUseCase submitOrderUseCase)
    {
        this.startOrderUseCase = startOrderUseCase;
        this.submitOrderUseCase = submitOrderUseCase;

        server.post("/api/partners/v1/orders", this::startOrder);
        server.put("/api/partners/v1/orders/{id}/submit", this::submitOrder);
    }

    private void startOrder(final Context context)
    {
        final OrderId orderId = startOrderUseCase.startOrder();

        context.status(HttpStatus.CREATED);
        context.json("""
                {"order_id":"%s"}
                """.formatted(orderId.value().toString())
        );
    }

    private void submitOrder(final Context context)
    {
        final OrderId orderId = new OrderId(UUID.fromString(context.pathParam("id")));

        try
        {
            submitOrderUseCase.submit(orderId);
            context.status(HttpStatus.OK);
        }
        catch (final OrderNotStartedException | NoPlacesAddedException e)
        {
            context.status(HttpStatus.CONFLICT);
        }
        catch (AggregateRestoreException e)
        {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
