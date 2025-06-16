package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import io.javalin.Javalin;
import io.javalin.http.Context;

public final class PlacesResourceJavalinHttpAdapter
{
    private final Javalin app;

    public PlacesResourceJavalinHttpAdapter(final Javalin app)
    {
        this.app = app;
        this.app.get("/api/public/v1/places", this::list);
    }

    private void list(final Context context)
    {
        context.json("""
                []
                """
        );
    }
}
