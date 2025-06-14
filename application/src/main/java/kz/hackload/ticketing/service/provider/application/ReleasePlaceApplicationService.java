package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;

public final class ReleasePlaceApplicationService implements ReleasePlaceUseCase
{
    private final ReleasePlaceService releasePlaceService;

    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;

    public ReleasePlaceApplicationService(final ReleasePlaceService releasePlaceService,
                                          final PlacesRepository placesRepository,
                                          final OrdersRepository ordersRepository)
    {
        this.releasePlaceService = releasePlaceService;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void releasePlace(final PlaceId placeId) throws OrderNotStartedException, PlaceNotAddedException
    {
        // TODO: throw place not found exception
        final Place place = placesRepository.findById(placeId).orElseThrow();
        final Order order = place.selectedFor().flatMap(ordersRepository::findById).orElseThrow();

        releasePlaceService.release(order, place);

        ordersRepository.save(order);
    }
}
