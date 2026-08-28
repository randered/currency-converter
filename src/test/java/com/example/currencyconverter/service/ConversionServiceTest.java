package com.example.currencyconverter.service;

import com.example.currencyconverter.common.ClientLockManager;
import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.config.ConversionLockConfig;
import com.example.currencyconverter.dto.BalanceDto;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.dto.CreateConversionRequest;
import com.example.currencyconverter.entity.ConversionRecord;
import com.example.currencyconverter.exception.ApiException;
import com.example.currencyconverter.exception.ConflictException;
import com.example.currencyconverter.repository.ConversionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock
    private ConversionRepository conversionRepository;
    @Mock
    private BalanceService balanceService;
    @Mock
    private ConversionTransactionService conversionTransactionService;

    private final ClientLockManager lockManager = new ClientLockManager();

    private ConversionService service;

    @BeforeEach
    void setUp() {
        service = new ConversionService(conversionRepository, balanceService, lockManager,
                new ConversionLockConfig(false), conversionTransactionService);
    }

    private CreateConversionRequest request(String from, String to, String amount) {
        return new CreateConversionRequest(from, to, new BigDecimal(amount));
    }

    private ConversionRecord record() {
        return ConversionRecord.builder()
                .transactionId(UUID.randomUUID())
                .clientId("CLIENT-001")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("100.00"))
                .targetAmount(new BigDecimal("85.00"))
                .rate(new BigDecimal("0.85"))
                .idempotencyKey(null)
                .build();
    }

    @Test
    void convertRunsInsideLockAndReturnsMappedResponse() {
        ConversionRecord record = record();
        when(conversionTransactionService.executeConversion(any(), any(), any(), any(), any()))
                .thenReturn(record);
        when(balanceService.getBalances("CLIENT-001")).thenReturn(List.of(
                new BalanceDto("USD", new BigDecimal("900.00")),
                new BalanceDto("EUR", new BigDecimal("85.00"))));

        ConversionResponse response = service.convert("CLIENT-001", null, request("USD", "EUR", "100.00"));

        assertThat(response.transactionId()).isEqualTo(record.getTransactionId());
        assertThat(response.targetAmount()).isEqualByComparingTo("85.00");
        assertThat(response.rate()).isEqualByComparingTo("0.85");
        assertThat(response.balances()).hasSize(2);
        verify(conversionTransactionService).executeConversion(any(), any(), any(), any(), any());
    }

    @Test
    void idempotencyReplayReturnsOriginalWithoutCallingTransaction() {
        ConversionRecord existing = ConversionRecord.builder()
                .transactionId(UUID.randomUUID())
                .clientId("CLIENT-001")
                .sourceCurrency("USD")
                .targetCurrency("EUR")
                .sourceAmount(new BigDecimal("100.00"))
                .targetAmount(new BigDecimal("85.00"))
                .rate(new BigDecimal("0.85"))
                .idempotencyKey("key-1")
                .build();
        when(conversionRepository.findByClientIdAndIdempotencyKey("CLIENT-001", "key-1"))
                .thenReturn(Optional.of(existing));
        when(balanceService.getBalances("CLIENT-001")).thenReturn(List.of());

        ConversionResponse response = service.convert("CLIENT-001", "key-1", request("USD", "EUR", "100.00"));

        assertThat(response.transactionId()).isEqualTo(existing.getTransactionId());
        verify(conversionTransactionService, never()).executeConversion(any(), any(), any(), any(), any());
    }

    @Test
    void conversionInProgressFailsFastWhenFlagEnabled() throws Exception {
        ConversionService flagged = new ConversionService(conversionRepository, balanceService, lockManager,
                new ConversionLockConfig(true), conversionTransactionService);
        // ReentrantLock is reentrant, so the lock has to be held by a different
        // thread than the one calling convert for tryLock() to fail.
        lockManager.lockFor("CLIENT-001").lock();
        try {
            AtomicReference<Throwable> thrown = new AtomicReference<>();
            Thread caller = new Thread(() -> {
                try {
                    flagged.convert("CLIENT-001", null, request("USD", "EUR", "100.00"));
                } catch (Throwable t) {
                    thrown.set(t);
                }
            });
            caller.setDaemon(true);
            caller.start();
            caller.join(1000);

            assertThat(thrown.get())
                    .isInstanceOf(ConflictException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONVERSION_IN_PROGRESS);
        } finally {
            lockManager.lockFor("CLIENT-001").unlock();
        }
    }

    @Test
    void historyRequiresAtLeastOneFilter() {
        assertThatThrownBy(() -> service.getHistory(null, null, null,
                org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("At least one filter");
    }

    @Test
    void historyRejectsMalformedTransactionId() {
        assertThatThrownBy(() -> service.getHistory("not-a-uuid", null, null,
                org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("valid UUID");
    }
}
