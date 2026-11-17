package com.luxeperfume.backend.checkout.domain;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public final class UuidOrderNumberGenerator implements OrderNumberGenerator {
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final Clock clock;

    public UuidOrderNumberGenerator() {
        this(Clock.systemUTC());
    }

    UuidOrderNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String nextOrderNumber() {
        String date = LocalDate.now(clock.withZone(ZoneOffset.UTC)).format(DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toUpperCase(Locale.ROOT);
        return "LP-" + date + "-" + suffix;
    }
}
