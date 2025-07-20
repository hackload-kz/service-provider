package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import java.util.List;
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
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.CreatePlaceRequest;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.CreatePlaceResponse;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.GetPlaceResponse;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.GetPlacesResponse;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto.SelectPlaceDto;

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

        app.post("/api/admin/v1/places", this::createPlace);
        app.patch("/api/partners/v1/places/{id}/select", this::selectPlace);
        app.patch("/api/partners/v1/places/{id}/release", this::releasePlace);
        app.get("/api/partners/v1/places/{id}", this::getPlace);
        app.get("/api/partners/v1/places", this::getPlaces);
    }

    private void createPlace(final Context context)
    {
        final CreatePlaceRequest createPlaceRequest;

        try
        {
            createPlaceRequest = context.bodyAsClass(CreatePlaceRequest.class);
        }
        catch (final Exception e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.UNPROCESSABLE_CONTENT);
            return;
        }

        try
        {
            final PlaceId placeId = createPlaceUseCase.create(createPlaceRequest.row(), createPlaceRequest.seat());
            final CreatePlaceResponse response = new CreatePlaceResponse(placeId);
            context.json(response);
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
        final PlaceId placeId;
        try
        {
            selectPlaceDto = context.bodyAsClass(SelectPlaceDto.class);
            placeId = new PlaceId(UUID.fromString(context.pathParam("id")));
        }
        catch (final Exception e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.UNPROCESSABLE_CONTENT);
            return;
        }

        try
        {
            selectPlaceUseCase.selectPlaceFor(placeId, selectPlaceDto.orderId());
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
        try
        {
            final PlaceId placeId = new PlaceId(UUID.fromString(context.pathParam("id")));
            removePlaceFromOrderUseCase.removePlaceFromOrder(placeId);
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
                final GetPlaceResponse response = new GetPlaceResponse(place.placeId(), place.row(), place.seat(), place.isFree());
                context.status(HttpStatus.OK);
                context.json(response);
            }, () -> context.status(HttpStatus.NOT_FOUND));
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void getPlaces(final Context context)
    {
        try
        {
            final int page = getPage(context);
            final int pageSize = getPageSize(context);

            final List<GetPlacesResponse.Place> places = getPlaceUseCase.getPlaces(page, pageSize).stream()
                    .map(p -> new GetPlacesResponse.Place(p.placeId(), p.row(), p.seat(), p.isFree()))
                    .toList();

            final GetPlacesResponse placesDto = new GetPlacesResponse(places);
            context.json(placesDto);
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static int getPage(final Context context)
    {
        final String pageQuery = context.queryParam("page");
        if (pageQuery == null)
        {
            return 1;
        }

        final int page = Integer.parseInt(pageQuery);

        return Math.max(page, 1);
    }

    private static int getPageSize(final Context context)
    {
        final String pageSizeQuery = context.queryParam("pageSize");
        if (pageSizeQuery == null)
        {
            return 20;
        }

        final int pageSize = Integer.parseInt(pageSizeQuery);

        return Math.min(pageSize, 100);
    }
}
