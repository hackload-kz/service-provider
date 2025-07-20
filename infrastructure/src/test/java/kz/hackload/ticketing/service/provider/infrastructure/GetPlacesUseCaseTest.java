package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlaceResourceJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesTestDto;

public class GetPlacesUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUo()
    {
        new PlaceResourceJavalinHttpAdapter(server, createPlaceUseCase, selectPlaceUseCase, removePlaceFromOrderUseCase, getPlaceUseCase);
    }

    @Test
    void placesAreReturned()
    {
        // given
        final ArrayList<PlaceId> placeIds = new ArrayList<>(10_000);

        for (int i = 0; i < 100; i++)
        {
            for (int j = 0; j < 100; j++)
            {
                final PlaceId placeId = createPlaceUseCase.create(new Row(i), new Seat(j));
                placeIds.add(placeId);
            }
        }

        JavalinTest.test(server, (s, c) ->
        {
            // when
            try (final Response response = c.get("/api/partners/v1/places?page=1&pageSize=20"))
            {
                // then
                assertThat(response.isSuccessful()).isTrue();
                try (final ResponseBody responseBody = response.body())
                {
                    assertThat(responseBody).isNotNull();
                    final PlacesTestDto placesDto = jsonMapper.fromJson(responseBody.string(), PlacesTestDto.class);
                    final List<? extends PlaceId> actual = assertThat(placesDto.places())
                            .hasSize(20)
                            .map(PlacesTestDto.PlaceTestDto::placeId)
                            .actual();

                    assertThat(placeIds.containsAll(actual)).isTrue();
                }
            }
        });
    }
}
