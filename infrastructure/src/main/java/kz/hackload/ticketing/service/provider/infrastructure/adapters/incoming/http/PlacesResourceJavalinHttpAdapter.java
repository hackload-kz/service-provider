package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import io.javalin.Javalin;
import io.javalin.http.Context;

public final class PlacesResourceJavalinHttpAdapter
{
    public PlacesResourceJavalinHttpAdapter(final Javalin app)
    {
        app.get("/api/public/v1/places", this::list);
    }

    private void list(final Context context)
    {
        context.json("""
                []
                """
        );
    }
}
