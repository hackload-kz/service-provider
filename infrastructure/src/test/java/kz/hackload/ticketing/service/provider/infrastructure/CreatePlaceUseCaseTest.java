package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public class CreatePlaceUseCaseTest extends AbstractIntegrationTest
{
    @Test
    void shouldCreatePlace()
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        // when
        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        // then
        final Place actual = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        assertThat(actual.id()).isEqualTo(placeId);
        assertThat(actual.isFree()).isTrue();
        assertThat(actual.selectedFor()).isEmpty();
    }
}
