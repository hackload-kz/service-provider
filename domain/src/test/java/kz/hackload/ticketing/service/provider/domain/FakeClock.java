package kz.hackload.ticketing.service.provider.domain;

import java.time.Clock;
import java.time.Instant;

public final class FakeClock implements Clocks
{
    private Clock clock = Clock.systemUTC();

    public void setClock(final Clock clock)
    {
        this.clock = clock;
    }

    public void resetClock()
    {
        this.clock = Clock.systemUTC();
    }

    @Override
    public Instant now()
    {
        return Instant.now(clock);
    }
}
