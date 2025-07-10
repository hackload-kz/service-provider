package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.RemovePlaceFromOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;

public final class PlaceResourceJavalinHttpAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(PlaceResourceJavalinHttpAdapter.class);

    private final SelectPlaceUseCase selectPlaceUseCase;
    private final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase;

    public PlaceResourceJavalinHttpAdapter(final Javalin app, final SelectPlaceUseCase selectPlaceUseCase, final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase)
    {
        this.selectPlaceUseCase = selectPlaceUseCase;
        this.removePlaceFromOrderUseCase = removePlaceFromOrderUseCase;

        app.patch("/api/partners/v1/places/{id}/select", this::selectPlace);
        app.patch("/api/partners/v1/places/{id}/release", this::releasePlace);
    }

    private void selectPlace(final Context context)
    {
        final SelectPlaceDto selectPlaceDto = context.bodyAsClass(SelectPlaceDto.class);

        try
        {
            selectPlaceUseCase.selectPlaceFor(selectPlaceDto.placeId(), selectPlaceDto.orderId());
            context.status(HttpStatus.NO_CONTENT);
        }
        catch (final PlaceAlreadySelectedException | PlaceCanNotBeAddedToOrderException e)
        {
            context.status(HttpStatus.CONFLICT);
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void releasePlace(final Context context)
    {
        final ReleasePlaceDto releasePlaceDto = context.bodyAsClass(ReleasePlaceDto.class);

        try
        {
            removePlaceFromOrderUseCase.removePlaceFromOrder(releasePlaceDto.placeId());
            context.status(HttpStatus.NO_CONTENT);
        }
        catch (final OrderNotStartedException | PlaceNotAddedException e)
        {
            context.status(HttpStatus.CONFLICT);
        }
        catch (final PlaceSelectedForAnotherOrderException e)
        {
            context.status(HttpStatus.FORBIDDEN);
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
