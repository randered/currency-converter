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

@Entity
@Table(name = "balances")
@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 50)
    String clientId;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false, precision = 24, scale = 2)
    BigDecimal amount;

    @Builder
    public Balance(String clientId, String currency, BigDecimal amount) {
        this.clientId = clientId;
        this.currency = currency;
        this.amount = amount;
    }

    public void applyDelta(BigDecimal delta) {
        this.amount = this.amount.add(delta);
    }
}
