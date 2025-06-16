package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class CreatePlaceUseCaseTest
{
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();

    private final CreatePlaceUseCase createPlaceUseCase = new CreatePlaceApplicationService(placesRepository);

    @Test
    void shouldCreatePlace() throws AggregateRestoreException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = placesRepository.nextId();

        // when
        createPlaceUseCase.create(placeId, row, seat);

        // then
        final Place actual = placesRepository.findById(placeId).orElseThrow();
        assertThat(actual.id()).isEqualTo(placeId);
        assertThat(actual.isFree()).isTrue();
        assertThat(actual.selectedFor()).isEmpty();
    }
}
