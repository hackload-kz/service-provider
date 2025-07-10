package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import java.util.UUID;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.GetPlaceUseCase;
import kz.hackload.ticketing.service.provider.application.RemovePlaceFromOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class PlaceResourceJavalinHttpAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(PlaceResourceJavalinHttpAdapter.class);

    private final CreatePlaceUseCase createPlaceUseCase;
    private final SelectPlaceUseCase selectPlaceUseCase;
    private final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase;
    private final GetPlaceUseCase getPlaceUseCase;

    public PlaceResourceJavalinHttpAdapter(final Javalin app,
                                           final CreatePlaceUseCase createPlaceUseCase,
                                           final SelectPlaceUseCase selectPlaceUseCase,
                                           final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase,
                                           final GetPlaceUseCase getPlaceUseCase)
    {
        this.createPlaceUseCase = createPlaceUseCase;
        this.selectPlaceUseCase = selectPlaceUseCase;
        this.removePlaceFromOrderUseCase = removePlaceFromOrderUseCase;
        this.getPlaceUseCase = getPlaceUseCase;

        app.post("/api/partners/v1/places", this::createPlace);
        app.patch("/api/partners/v1/places/{id}/select", this::selectPlace);
        app.patch("/api/partners/v1/places/{id}/release", this::releasePlace);
        app.get("/api/partners/v1/places/{id}", this::getPlace);
    }

    private void createPlace(final Context context)
    {
        final CreatePlaceDto createPlaceDto;

        try
        {
            createPlaceDto = context.bodyAsClass(CreatePlaceDto.class);
        }
        catch (final Exception e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.UNPROCESSABLE_CONTENT);
            return;
        }

        try
        {
            createPlaceUseCase.create(createPlaceDto.row(), createPlaceDto.seat());
            context.status(HttpStatus.ACCEPTED);
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void selectPlace(final Context context)
    {
        final SelectPlaceDto selectPlaceDto;
        try
        {
            selectPlaceDto = context.bodyAsClass(SelectPlaceDto.class);
        }
        catch (final Exception e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.UNPROCESSABLE_CONTENT);
            return;
        }

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

    private void getPlace(final Context context)
    {
        final PlaceId placeId = new PlaceId(UUID.fromString(context.pathParam("id")));

        try
        {
            getPlaceUseCase.getPlace(placeId).ifPresentOrElse(place ->
            {
                context.json("""
                        {
                            "id": "%s",
                            "row": %s,
                            "seat": %s,
                            "is_free": %s
                        }
                        """.formatted(place.placeId(), place.row(), place.seat(), place.isFree()));
                context.status(HttpStatus.OK);
            }, () -> context.status(HttpStatus.NOT_FOUND));
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
