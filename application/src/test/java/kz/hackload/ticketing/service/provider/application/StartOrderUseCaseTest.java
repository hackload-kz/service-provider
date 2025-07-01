package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public class StartOrderUseCaseTest
{
    private final OrdersRepository ordersRepository = new OrdersRepositoryInMemoryAdapter();
    private final TransactionManager transactionManager = new NoopTransactionManager();
    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);

    @Test
    void shouldStartOrder() throws AggregateRestoreException
    {
        final OrderId orderId = startOrderUseCase.startOrder();

        final Optional<Order> optionalOrder = ordersRepository.findById(orderId);

        final Order actual = assertThat(optionalOrder)
                .isPresent()
                .get()
                .actual();

        assertThat(actual.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(actual.places()).isEmpty();
        assertThat(actual.uncommittedEvents()).isEmpty();
    }
}
