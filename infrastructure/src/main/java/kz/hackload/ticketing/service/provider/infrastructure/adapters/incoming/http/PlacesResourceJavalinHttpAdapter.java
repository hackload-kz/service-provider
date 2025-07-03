package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;

public final class PlacesResourceJavalinHttpAdapter
{
    private final SelectPlaceUseCase selectPlaceUseCase;
    private final ReleasePlaceUseCase releasePlaceUseCase;

    public PlacesResourceJavalinHttpAdapter(final Javalin app, final SelectPlaceUseCase selectPlaceUseCase, final ReleasePlaceUseCase releasePlaceUseCase)
    {
        this.selectPlaceUseCase = selectPlaceUseCase;
        this.releasePlaceUseCase = releasePlaceUseCase;

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
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void releasePlace(final Context context)
    {
        final ReleasePlaceDto releasePlaceDto = context.bodyAsClass(ReleasePlaceDto.class);

        try
        {
            releasePlaceUseCase.releasePlace(releasePlaceDto.placeId());
            context.status(HttpStatus.NO_CONTENT);
        }
        catch (final OrderNotStartedException | PlaceNotAddedException | PlaceAlreadyReleasedException e)
        {
            context.status(HttpStatus.CONFLICT);
        }
        catch (final RuntimeException e)
        {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
