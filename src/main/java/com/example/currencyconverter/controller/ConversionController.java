package com.example.currencyconverter.controller;

import com.example.currencyconverter.dto.ConversionDto;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.dto.CreateConversionRequest;
import com.example.currencyconverter.exception.ValidationException;
import com.example.currencyconverter.service.ConversionService;
import com.example.currencyconverter.util.Constants;
import com.example.currencyconverter.util.InputUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/conversions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversionController {

    ConversionService conversionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversionResponse convert(
            @RequestHeader(Constants.HEADER_CLIENT_ID) String clientId,
            @RequestHeader(value = Constants.HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody CreateConversionRequest request) {
        if (clientId.isBlank()) {
            throw new ValidationException(Constants.HEADER_CLIENT_ID_REQUIRED);
        }
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > 128)) {
            throw new ValidationException(Constants.HEADER_IDEMPOTENCY_KEY_INVALID);
        }
        return conversionService.convert(clientId.trim(), InputUtils.trimToNull(idempotencyKey), request);
    }

    @GetMapping
    public PagedModel<ConversionDto> getHistory(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String clientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ConversionDto> page = conversionService.getHistory(
                InputUtils.trimToNull(transactionId), date, InputUtils.trimToNull(clientId), pageable);
        return new PagedModel<>(page);
    }
}
