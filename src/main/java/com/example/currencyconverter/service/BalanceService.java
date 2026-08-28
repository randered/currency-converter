package com.example.currencyconverter.service;

import com.example.currencyconverter.repository.BalanceRepository;
import com.example.currencyconverter.repository.ClientRepository;
import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.dto.BalanceDto;
import com.example.currencyconverter.exception.NotFoundException;
import com.example.currencyconverter.util.Constants;
import com.example.currencyconverter.util.LogMessages;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    ClientRepository clientRepository;
    BalanceRepository balanceRepository;

    @Transactional(readOnly = true)
    public List<BalanceDto> getBalances(String clientId) {
        clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLIENT_NOT_FOUND,
                        Constants.CLIENT_NOT_FOUND.formatted(clientId)));
        List<BalanceDto> balances = balanceRepository.findByClientIdOrderByCurrencyAsc(clientId).stream()
                .map(balance -> new BalanceDto(balance.getCurrency(), balance.getAmount()))
                .toList();
        log.info(LogMessages.BALANCES, clientId, balances.size());
        return balances;
    }
}
