package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;

public class PlaceResourcesTest
{
    private PlacesResourceJavalinHttpAdapter adapter;
    private Javalin server;

    @BeforeEach
    public void setUp()
    {
        server = Javalin.create();
        adapter = new PlacesResourceJavalinHttpAdapter(server);
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
}
