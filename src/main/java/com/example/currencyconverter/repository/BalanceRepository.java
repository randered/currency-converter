package com.example.currencyconverter.repository;

import com.example.currencyconverter.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    Optional<Balance> findByClientIdAndCurrency(String clientId, String currency);

    List<Balance> findByClientIdOrderByCurrencyAsc(String clientId);
}
