package com.example.currencyconverter.service;

import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.dto.RateResponse;
import com.example.currencyconverter.entity.Balance;
import com.example.currencyconverter.entity.Client;
import com.example.currencyconverter.entity.ConversionRecord;
import com.example.currencyconverter.exception.ApiException;
import com.example.currencyconverter.exception.InsufficientFundsException;
import com.example.currencyconverter.exception.NotFoundException;
import com.example.currencyconverter.repository.BalanceRepository;
import com.example.currencyconverter.repository.ClientRepository;
import com.example.currencyconverter.repository.ConversionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionTransactionServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private BalanceRepository balanceRepository;
    @Mock
    private ConversionRepository conversionRepository;
    @Mock
    private RateService rateService;

    private ConversionTransactionService service;

    @BeforeEach
    void setUp() {
        service = new ConversionTransactionService(clientRepository, balanceRepository,
                conversionRepository, rateService);
    }

    @Test
    void happyPathDebitsSourceCreditsTargetAndPersists() {
        Balance usd = new Balance("CLIENT-001", "USD", new BigDecimal("1000.00"));
        Balance eur = new Balance("CLIENT-001", "EUR", new BigDecimal("0.00"));
        when(clientRepository.findByClientId("CLIENT-001")).thenReturn(Optional.of(new Client("CLIENT-001")));
        when(balanceRepository.findByClientIdAndCurrency("CLIENT-001", "USD")).thenReturn(Optional.of(usd));
        when(rateService.getRate("USD", "EUR")).thenReturn(
                new RateResponse("USD", "EUR", new BigDecimal("0.85"), Instant.now()));
        when(balanceRepository.findByClientIdAndCurrency("CLIENT-001", "EUR")).thenReturn(Optional.of(eur));
        when(conversionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversionRecord record = service.executeConversion(
                "CLIENT-001", null, "USD", "EUR", new BigDecimal("100.00"));

        assertThat(record.getTransactionId()).isNotNull();
        assertThat(record.getTargetAmount()).isEqualByComparingTo("85.00");
        assertThat(usd.getAmount()).isEqualByComparingTo("900.00");
        assertThat(eur.getAmount()).isEqualByComparingTo("85.00");
        verify(conversionRepository).saveAndFlush(any(ConversionRecord.class));
    }

    @Test
    void insufficientFundsIsRejectedAndNothingPersisted() {
        Balance usd = new Balance("CLIENT-001", "USD", new BigDecimal("10.00"));
        when(clientRepository.findByClientId("CLIENT-001")).thenReturn(Optional.of(new Client("CLIENT-001")));
        when(balanceRepository.findByClientIdAndCurrency("CLIENT-001", "USD")).thenReturn(Optional.of(usd));
        when(rateService.getRate("USD", "EUR")).thenReturn(
                new RateResponse("USD", "EUR", new BigDecimal("0.85"), Instant.now()));

        assertThatThrownBy(() -> service.executeConversion(
                "CLIENT-001", null, "USD", "EUR", new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);

        verify(conversionRepository, never()).saveAndFlush(any());
        assertThat(usd.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void unknownClientIsRejected() {
        when(clientRepository.findByClientId("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executeConversion(
                "NOPE", null, "USD", "EUR", new BigDecimal("10.00")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLIENT_NOT_FOUND);
    }

    @Test
    void missingSourceBalanceIsRejected() {
        when(clientRepository.findByClientId("CLIENT-001")).thenReturn(Optional.of(new Client("CLIENT-001")));
        when(balanceRepository.findByClientIdAndCurrency("CLIENT-001", "GBP")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executeConversion(
                "CLIENT-001", null, "GBP", "USD", new BigDecimal("10.00")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.BALANCE_NOT_FOUND);
    }

    @Test
    void reCheckReplayReturnsExistingRecord() {
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

        ConversionRecord result = service.executeConversion(
                "CLIENT-001", "key-1", "USD", "EUR", new BigDecimal("100.00"));

        assertThat(result).isSameAs(existing);
        verify(conversionRepository, never()).saveAndFlush(any());
    }
}
