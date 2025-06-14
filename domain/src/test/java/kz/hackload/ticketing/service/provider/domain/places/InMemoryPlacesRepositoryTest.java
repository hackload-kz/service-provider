package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ConcurrentModificationException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.InMemoryPlacesRepository;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public class InMemoryPlacesRepositoryTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final PlacesRepository repository = new InMemoryPlacesRepository();

    @Test
    void shouldSaveCreatedPlace()
    {
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        final Place place = Place.create(placeId);
        repository.save(place);
        assertThat(place.uncommittedEvents()).isEmpty();

        final Optional<Place> optionalFoundPlace = repository.findById(placeId);
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
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));

        final Place place1 = Place.create(placeId);
        repository.save(place1);

        final Place place2 = Place.create(placeId);

        assertThatThrownBy(() -> repository.save(place2))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessage("Revision mismatch exception for %s".formatted(placeId));
    }

    @Test
    void shouldSaveSelectedPlace() throws PlaceAlreadySelectedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        final Place place = Place.create(placeId);
        place.selectFor(orderId);
        repository.save(place);
        assertThat(place.uncommittedEvents()).isEmpty();

        final Optional<Place> optionalFoundPlace = repository.findById(placeId);
        assertThat(optionalFoundPlace).isPresent();

        final Place foundPlace = optionalFoundPlace.get();
        assertThat(foundPlace.id()).isEqualTo(placeId);
        assertThat(foundPlace.isFree()).isFalse();
        assertThat(foundPlace.selectedFor()).isPresent().get().isEqualTo(orderId);
        assertThat(foundPlace.uncommittedEvents()).isEmpty();
        assertThat(foundPlace.revision()).isEqualTo(2L);
    }

    @Test
    void shouldSaveReleasedPlace() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        final Place place = Place.create(placeId);
        place.selectFor(orderId);
        repository.save(place);
        assertThat(place.uncommittedEvents()).isEmpty();

        final Place selectedPlace = repository.findById(placeId).orElseThrow();
        selectedPlace.release();

        repository.save(selectedPlace);

        final Optional<Place> optionalFoundPlace = repository.findById(placeId);
        assertThat(optionalFoundPlace).isPresent();

        final Place foundPlace = optionalFoundPlace.get();
        assertThat(foundPlace.id()).isEqualTo(placeId);
        assertThat(foundPlace.isFree()).isTrue();
        assertThat(foundPlace.selectedFor()).isEmpty();
        assertThat(foundPlace.uncommittedEvents()).isEmpty();
        assertThat(foundPlace.revision()).isEqualTo(3L);
    }
}
