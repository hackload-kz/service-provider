package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.shaded.org.awaitility.Awaitility;

import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlaceResourceJavalinHttpAdapter;

public class CreatePlaceUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new PlaceResourceJavalinHttpAdapter(server, createPlaceUseCase, selectPlaceUseCase, removePlaceFromOrderUseCase, getPlaceUseCase);
    }

    @Test
    void shouldCreatePlace()
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        JavalinTest.test(server, (_, c) ->
        {
            // when
            try (Response response = c.post("/api/admin/places", """
                    {
                        "row": %s,
                        "seat": %s
                    }
                    """.formatted(row, seat)))
            {
                assertThat(response.isSuccessful()).isTrue();
                final ResponseBody responseBody = response.body();
                assertThat(responseBody).isNotNull();

                final Map<?, ?> map = jsonMapper.fromJson(responseBody.string(), Map.class);
                final PlaceId placeId = new PlaceId(UUID.fromString((String) map.get("place_id")));

                Awaitility.await()
                        .atMost(Duration.ofSeconds(10L))
                        .until(() -> placesQueryRepository.getPlace(placeId).isPresent());

                // then
                final Place actual = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
                assertThat(actual.id()).isEqualTo(placeId);
                assertThat(actual.isFree()).isTrue();
                assertThat(actual.selectedFor()).isEmpty();

                final GetPlaceQueryResult getPlaceQueryResult = transactionManager.executeInTransaction(() -> placesQueryRepository.getPlace(placeId)).orElseThrow();
                assertThat(getPlaceQueryResult).isEqualTo(new GetPlaceQueryResult(placeId, row, seat, true));
            }
        });
    }
}
