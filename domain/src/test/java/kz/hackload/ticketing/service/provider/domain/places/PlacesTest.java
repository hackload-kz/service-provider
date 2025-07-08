package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.DomainEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public class PlacesTest
{
    private final OrderId orderId = new OrderId(UUID.randomUUID());

    @Test
    void shouldCreateFreePlace()
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);

        assertThat(place.id()).isEqualTo(placeId);
        assertThat(place.row()).isEqualTo(row);
        assertThat(place.seat()).isEqualTo(seat);
        assertThat(place.isFree()).isTrue();
    }

    @Test
    void shouldSelectPlace() throws PlaceAlreadySelectedException
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);
        place.commitEvents();

        place.selectFor(now, orderId);

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
                .isEqualTo(new PlaceSelectedEvent(now, 2, orderId));
    }

    @Test
    void shouldReleasePlace() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);
        place.commitEvents();

        place.selectFor(now, orderId);
        place.release(now);

        assertThat(place.selectedFor()).isEmpty();
        assertThat(place.isFree()).isTrue();

        assertThat(place.uncommittedEvents())
                .hasSize(2)
                .containsExactly(new PlaceSelectedEvent(now, 2, orderId), new PlaceReleasedEvent(now, 3, orderId));
    }

    @Test
    void shouldNotSelectAlreadySelectedPlace() throws PlaceAlreadySelectedException
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);

        place.selectFor(now, orderId);

        assertThatThrownBy(() -> place.selectFor(now, orderId))
                .isInstanceOf(PlaceAlreadySelectedException.class)
                .hasMessage("The place %s is already selected".formatted(placeId));
    }

    @Test
    void shouldNotReleaseAlreadyReleasedPlace() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);

        place.selectFor(now, orderId);
        place.release(now);

        assertThatThrownBy(() -> place.release(now))
                .isInstanceOf(PlaceAlreadyReleasedException.class)
                .hasMessage("The place %s is already released".formatted(placeId));
    }

    @Test
    void shouldCreatePlace()
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);

        assertThat(place.isFree()).isTrue();
        assertThat(place.selectedFor()).isEmpty();
        assertThat(place.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceCreatedEvent(now, 1L, row, seat));
    }

    @Test
    void shouldRestorePlaceFromEvents()
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());

        final PlaceCreatedEvent event = new PlaceCreatedEvent(now, 1, row, seat);

        final Place place = Place.restore(placeId, List.of(event));
        assertThat(place.id()).isEqualTo(placeId);
        assertThat(place.isFree()).isTrue();
        assertThat(place.selectedFor()).isEmpty();
        assertThat(place.uncommittedEvents()).isEmpty();
        assertThat(place.revision()).isEqualTo(1L);
    }

    @Test
    void shouldNotRestorePlaceIfEventMismatched()
    {
        final Instant now = Instant.now();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());

        final PlaceCreatedEvent event = new PlaceCreatedEvent(now, 0, row, seat);
        final PlaceSelectedEvent placeSelectedEvent1 = new PlaceSelectedEvent(now, 1, orderId);
        final PlaceSelectedEvent placeSelectedEvent2 = new PlaceSelectedEvent(now, 2, orderId);

        final List<PlaceDomainEvent> events = List.of(event, placeSelectedEvent1, placeSelectedEvent2);

        assertThatThrownBy(() -> Place.restore(placeId, events))
                .isInstanceOf(AggregateRestoreException.class)
                .hasMessage("kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException: The place %s is already selected".formatted(Objects.requireNonNull(placeId)));
    }
}
