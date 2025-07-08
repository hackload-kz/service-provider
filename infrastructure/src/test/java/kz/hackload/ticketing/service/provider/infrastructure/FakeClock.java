package kz.hackload.ticketing.service.provider.infrastructure;

import java.time.Clock;
import java.time.Instant;

import kz.hackload.ticketing.service.provider.domain.Clocks;

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
