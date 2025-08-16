package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import java.util.UUID;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.CancelOrderUseCase;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderUseCase;
import kz.hackload.ticketing.service.provider.application.GetOrderUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SubmitOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.ConfirmedOrderCanNotBeCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderAlreadyCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotSubmittedException;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.GetOrderResponse;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.StartOrderResponse;

public class OrderResourcesJavalinHttpAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(OrderResourcesJavalinHttpAdapter.class);

    private final StartOrderUseCase startOrderUseCase;
    private final SubmitOrderUseCase submitOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderResourcesJavalinHttpAdapter(final Javalin server,
                                            final StartOrderUseCase startOrderUseCase,
                                            final SubmitOrderUseCase submitOrderUseCase,
                                            final ConfirmOrderUseCase confirmOrderUseCase,
                                            final CancelOrderUseCase cancelOrderUseCase,
                                            final GetOrderUseCase getOrderUseCase)
    {
        this.startOrderUseCase = startOrderUseCase;
        this.submitOrderUseCase = submitOrderUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;

        server.post("/api/partners/v1/orders", this::startOrder);
        server.patch("/api/partners/v1/orders/{id}/submit", this::submitOrder);
        server.patch("/api/partners/v1/orders/{id}/confirm", this::confirmOrder);
        server.patch("/api/partners/v1/orders/{id}/cancel", this::cancelOrder);

        server.get("/api/partners/v1/orders/{id}", this::getOrder);
    }

    private void startOrder(final Context context)
    {
        try
        {
            final OrderId orderId = startOrderUseCase.startOrder();
            final StartOrderResponse response = new StartOrderResponse(orderId);
            context.status(HttpStatus.CREATED);
            context.json(response);
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
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
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void cancelOrder(final Context context)
    {
        final OrderId orderId = new OrderId(UUID.fromString(context.pathParam("id")));

        try
        {
            cancelOrderUseCase.cancel(orderId);
            context.status(HttpStatus.OK);
        }
        catch (final OrderAlreadyCancelledException | ConfirmedOrderCanNotBeCancelledException e)
        {
            context.status(HttpStatus.CONFLICT);
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void getOrder(final Context context)
    {
        final String orderIdParam = context.pathParam("id");

        try
        {
            final OrderId orderId = new OrderId(UUID.fromString(orderIdParam));
            getOrderUseCase.getOrder(orderId).ifPresentOrElse(order ->
                    {
                        final GetOrderResponse response = new GetOrderResponse(
                                order.id(),
                                order.status(),
                                order.startedAt(),
                                order.updatedAt(),
                                order.placesCount()
                        );

                        context.status(HttpStatus.OK);
                        context.json(response);
                    },
                    () -> context.status(HttpStatus.NOT_FOUND));
        }
        catch (final RuntimeException e)
        {
            LOG.error("Failed to process request to order id " + orderIdParam + ": " + e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
