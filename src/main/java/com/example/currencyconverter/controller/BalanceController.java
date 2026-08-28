package com.example.currencyconverter.controller;

import com.example.currencyconverter.dto.BalanceDto;
import com.example.currencyconverter.service.BalanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clients/{clientId}/balances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BalanceController {

    BalanceService balanceService;

    @GetMapping
    public List<BalanceDto> getBalances(@PathVariable String clientId) {
        return balanceService.getBalances(clientId);
    }
}
