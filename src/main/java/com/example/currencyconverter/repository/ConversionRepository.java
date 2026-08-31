package com.example.currencyconverter.repository;

import com.example.currencyconverter.entity.ConversionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ConversionRepository extends JpaRepository<ConversionRecord, Long>,
        JpaSpecificationExecutor<ConversionRecord> {

    Optional<ConversionRecord> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);
}
