package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.DomainEvent;
import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.InMemoryPlacesRepository;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public class PlacesTest
{
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final OrderId orderId = ordersRepository.nextId();

    @Test
    void shouldCreateFreePlace()
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);

        assertThat(place.id()).isEqualTo(placeId);
        assertThat(place.row()).isEqualTo(row);
        assertThat(place.seat()).isEqualTo(seat);
        assertThat(place.isFree()).isTrue();
    }

    @Test
    void shouldSelectPlace() throws PlaceAlreadySelectedException
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);
        place.commitEvents();

        place.selectFor(orderId);

        assertThat(place.isSelectedFor(orderId)).isTrue();
        assertThat(place.selectedFor())
                .isPresent()
                .get()
                .isEqualTo(orderId);
        assertThat(place.isFree()).isFalse();

        final List<? extends DomainEvent> events = place.uncommittedEvents();

        assertThat(events)
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceSelectedEvent(orderId));
    }

    @Test
    void shouldReleasePlace() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);
        place.commitEvents();

        place.selectFor(orderId);
        place.release();

        assertThat(place.selectedFor()).isEmpty();
        assertThat(place.isFree()).isTrue();

        assertThat(place.uncommittedEvents())
                .hasSize(2)
                .containsExactly(new PlaceSelectedEvent(orderId), new PlaceReleasedEvent(orderId));
    }

    @Test
    void shouldNotSelectAlreadySelectedPlace() throws PlaceAlreadySelectedException
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);

        place.selectFor(orderId);

        assertThatThrownBy(() -> place.selectFor(orderId))
                .isInstanceOf(PlaceAlreadySelectedException.class)
                .hasMessage("The place %s is already selected".formatted(placeId));
    }

    @Test
    void shouldNotReleaseAlreadyReleasedPlace() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);

        place.selectFor(orderId);
        place.release();

        assertThatThrownBy(place::release)
                .isInstanceOf(PlaceAlreadyReleasedException.class)
                .hasMessage("The place %s is already released".formatted(placeId));
    }

    @Test
    void shouldCreatePlace()
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);

        assertThat(place.isFree()).isTrue();
        assertThat(place.selectedFor()).isEmpty();
        assertThat(place.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceCreatedEvent(row, seat));
    }

    @Test
    void shouldRestorePlaceFromEvents() throws AggregateRestoreException
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();

        final PlaceCreatedEvent event = new PlaceCreatedEvent(row, seat);

        final Place place = Place.restore(placeId, 1L, List.of(event));
        assertThat(place.id()).isEqualTo(placeId);
        assertThat(place.isFree()).isTrue();
        assertThat(place.selectedFor()).isEmpty();
        assertThat(place.uncommittedEvents()).isEmpty();
        assertThat(place.revision()).isEqualTo(1L);
    }

    @Test
    void shouldNotRestorePlaceIfEventMismatched()
    {
        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();

        final PlaceCreatedEvent event = new PlaceCreatedEvent(row, seat);
        final PlaceSelectedEvent placeSelectedEvent1 = new PlaceSelectedEvent(orderId);
        final PlaceSelectedEvent placeSelectedEvent2 = new PlaceSelectedEvent(orderId);

        final List<PlaceDomainEvent> events = List.of(event, placeSelectedEvent1, placeSelectedEvent2);

        assertThatThrownBy(() -> Place.restore(placeId, 1L, events))
                .isInstanceOf(AggregateRestoreException.class)
                .hasMessage("kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException: The place %s is already selected".formatted(Objects.requireNonNull(placeId)));
    }
}
