package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.OrdersRepositoryInMemoryAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.PlacesRepositoryInMemoryAdapter;
import okhttp3.Response;

public class PlaceResourcesTest
{
    private final OrdersRepository ordersRepository = new OrdersRepositoryInMemoryAdapter();
    private final PlacesRepository placesRepository = new PlacesRepositoryInMemoryAdapter();

    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final ReleasePlaceService releasePlaceService = new ReleasePlaceService();

    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(releasePlaceService, placesRepository, ordersRepository);

    private PlacesResourceJavalinHttpAdapter adapter;
    private Javalin server;

    @BeforeEach
    public void setUp()
    {
        server = Javalin.create();
        adapter = new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, releasePlaceUseCase);
    }

    @AfterEach
    public void tearDown()
    {
        server.stop();
    }

    @Test
    void shouldListPlacesBySector()
    {
        JavalinTest.test(server, (s, c) ->
        {
            final Response response = c.get("/api/public/v1/places");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("""
                    []
                    """
            );
        });
    }

    @Test
    void placeSelected()
    {
        final PlaceId placeId = placesRepository.nextId();

        final Place place = Place.create(placeId, new Row(1), new Seat(1));
        placesRepository.save(place);

        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        ordersRepository.save(order);

        JavalinTest.test(server, (s, c) ->
        {
            try (final Response response = c.patch("/api/partners/v1/places/" + placeId.value() + "/select", """
                    {
                        "order_id": "%s",
                        "place_id": "%s"
                    }
                    """.formatted(orderId.value(), placeId.value())))
            {
                assertThat(response.isSuccessful()).isTrue();
                final Optional<Place> optionalPlace = placesRepository.findById(placeId);

                final Place actual = assertThat(optionalPlace)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(actual.isFree()).isFalse();
                assertThat(actual.selectedFor()).isPresent().get().isEqualTo(orderId);
                assertThat(actual.isSelectedFor(orderId)).isTrue();
            }
        });
    }

    @Test
    void placeReleased() throws PlaceAlreadySelectedException, OrderNotStartedException, PlaceAlreadyAddedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = placesRepository.nextId();
        final Place place = Place.create(placeId, new Row(1), new Seat(1));

        place.selectFor(orderId);
        order.addPlace(placeId);

        ordersRepository.save(order);
        placesRepository.save(place);

        JavalinTest.test(server, (s, c) ->
        {
            try (final Response response = c.patch("/api/partners/v1/places/" + placeId.value() + "/release", """
                    {
                        "order_id": "%s",
                        "place_id": "%s"
                    }
                    """.formatted(orderId.value(), placeId.value())))
            {
                assertThat(response.isSuccessful()).isTrue();
                final Optional<Order> optionalOrder = ordersRepository.findById(orderId);

                final Order actual = assertThat(optionalOrder)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(actual.contains(placeId)).isFalse();
                assertThat(actual.places()).isEmpty();
            }
        });
    }
}
