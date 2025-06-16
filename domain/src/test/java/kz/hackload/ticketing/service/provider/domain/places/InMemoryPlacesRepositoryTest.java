package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ConcurrentModificationException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.InMemoryPlacesRepository;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public class InMemoryPlacesRepositoryTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();

    @Test
    void shouldSaveCreatedPlace() throws AggregateRestoreException
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);
        placesRepository.save(place);
        assertThat(place.uncommittedEvents()).isEmpty();

        final Optional<Place> optionalFoundPlace = placesRepository.findById(placeId);
        assertThat(optionalFoundPlace).isPresent();

        final Place foundPlace = optionalFoundPlace.get();
        assertThat(foundPlace.id()).isEqualTo(placeId);
        assertThat(foundPlace.isFree()).isTrue();
        assertThat(foundPlace.selectedFor()).isEmpty();
        assertThat(foundPlace.uncommittedEvents()).isEmpty();
        assertThat(foundPlace.revision()).isEqualTo(1L);
    }

    @Test
    void shouldNotSaveTwoCreatedPlacesWithSameId()
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place1 = Place.create(placeId, row, seat);
        placesRepository.save(place1);

        final Place place2 = Place.create(placeId, row, seat);

        assertThatThrownBy(() -> placesRepository.save(place2))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessage("Revision mismatch exception for %s".formatted(placeId));
    }

    @Test
    void shouldSaveSelectedPlace() throws PlaceAlreadySelectedException, AggregateRestoreException
    {
        final OrderId orderId = ordersRepository.nextId();
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);
        place.selectFor(orderId);
        placesRepository.save(place);
        assertThat(place.uncommittedEvents()).isEmpty();

        final Optional<Place> optionalFoundPlace = placesRepository.findById(placeId);
        assertThat(optionalFoundPlace).isPresent();

        final Place foundPlace = optionalFoundPlace.get();
        assertThat(foundPlace.id()).isEqualTo(placeId);
        assertThat(foundPlace.isFree()).isFalse();
        assertThat(foundPlace.selectedFor()).isPresent().get().isEqualTo(orderId);
        assertThat(foundPlace.uncommittedEvents()).isEmpty();
        assertThat(foundPlace.revision()).isEqualTo(2L);
    }

    @Test
    void shouldSaveReleasedPlace() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException, AggregateRestoreException
    {
        final OrderId orderId = ordersRepository.nextId();
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);
        place.selectFor(orderId);
        placesRepository.save(place);
        assertThat(place.uncommittedEvents()).isEmpty();

        final Place selectedPlace = placesRepository.findById(placeId).orElseThrow();
        selectedPlace.release();

        placesRepository.save(selectedPlace);

        final Optional<Place> optionalFoundPlace = placesRepository.findById(placeId);
        assertThat(optionalFoundPlace).isPresent();

        final Place foundPlace = optionalFoundPlace.get();
        assertThat(foundPlace.id()).isEqualTo(placeId);
        assertThat(foundPlace.isFree()).isTrue();
        assertThat(foundPlace.selectedFor()).isEmpty();
        assertThat(foundPlace.uncommittedEvents()).isEmpty();
        assertThat(foundPlace.revision()).isEqualTo(3L);
    }
}
