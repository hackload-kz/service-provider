package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;

public class InMemoryOrdersRepositoryTest
{
    private final OrdersRepository repository = new InMemoryOrdersRepository();

    @Test
    void shouldNotReturnIdenticalIds()
    {
        assertThat(repository.nextId())
                .isNotEqualTo(repository.nextId());
    }
}
