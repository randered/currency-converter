package com.example.currencyconverter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversion_records")
@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    UUID transactionId;

    @Column(nullable = false, length = 50)
    String clientId;

    @Column(nullable = false, length = 3)
    String sourceCurrency;

    @Column(nullable = false, length = 3)
    String targetCurrency;

    @Column(nullable = false, precision = 24, scale = 2)
    BigDecimal sourceAmount;

    @Column(nullable = false, precision = 24, scale = 2)
    BigDecimal targetAmount;

    @Column(nullable = false, precision = 24, scale = 10)
    BigDecimal rate;

    @Column(length = 128)
    String idempotencyKey;

    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @Builder
    public ConversionRecord(UUID transactionId,
                            String clientId,
                            String sourceCurrency,
                            String targetCurrency,
                            BigDecimal sourceAmount,
                            BigDecimal targetAmount,
                            BigDecimal rate,
                            String idempotencyKey) {
        this.transactionId = transactionId;
        this.clientId = clientId;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.sourceAmount = sourceAmount;
        this.targetAmount = targetAmount;
        this.rate = rate;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }
}
