package com.example.currencyconverter.controller;

import com.example.currencyconverter.dto.RateResponse;
import com.example.currencyconverter.service.RateService;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rates")
@Validated
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateController {

    RateService rateService;

    @GetMapping
    public RateResponse getRate(
            @RequestParam @Pattern(regexp = "[A-Za-z]{3}",
                    message = "from must be a 3-letter ISO-4217 code") String from,
            @RequestParam @Pattern(regexp = "[A-Za-z]{3}",
                    message = "to must be a 3-letter ISO-4217 code") String to) {
        return rateService.getRate(from, to);
    }
}
