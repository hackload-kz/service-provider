package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import java.util.UUID;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SubmitOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotSubmittedException;

public class OrderResourcesJavalinHttpAdapter
{
    private final StartOrderUseCase startOrderUseCase;
    private final SubmitOrderUseCase submitOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;

    public OrderResourcesJavalinHttpAdapter(final Javalin server,
                                            final StartOrderUseCase startOrderUseCase,
                                            final SubmitOrderUseCase submitOrderUseCase,
                                            final ConfirmOrderUseCase confirmOrderUseCase)
    {
        this.startOrderUseCase = startOrderUseCase;
        this.submitOrderUseCase = submitOrderUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;

        server.post("/api/partners/v1/orders", this::startOrder);
        server.patch("/api/partners/v1/orders/{id}/submit", this::submitOrder);
        server.patch("/api/partners/v1/orders/{id}/confirm", this::confirmOrder);
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
        catch (final AggregateRestoreException e)
        {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void confirmOrder(final Context context)
    {
        final OrderId orderId = new OrderId(UUID.fromString(context.pathParam("id")));

        try
        {
            confirmOrderUseCase.confirm(orderId);
            context.status(HttpStatus.OK);
        }
        catch (final OrderNotSubmittedException e)
        {
            context.status(HttpStatus.CONFLICT);
        }
        catch (final AggregateRestoreException e)
        {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
