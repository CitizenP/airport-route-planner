package com.airportrouteplanner.routeservice.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class FixedOffsetUtcConverterTests {

    private final FixedOffsetUtcConverter converter = new FixedOffsetUtcConverter();

    @ParameterizedTest
    @MethodSource("offsetExamples")
    void convertsOriginLocalTimeUsingOnlyItsFixedOffset(
            int offsetMinutes, String expectedInstant) {
        LocalDateTime local = LocalDateTime.of(2026, 10, 20, 16, 0);

        assertThat(converter.toUtcInstant(local, offsetMinutes))
                .isEqualTo(Instant.parse(expectedInstant));
    }

    private static Stream<Arguments> offsetExamples() {
        return Stream.of(
                Arguments.of(0, "2026-10-20T16:00:00Z"),
                Arguments.of(120, "2026-10-20T14:00:00Z"),
                Arguments.of(-300, "2026-10-20T21:00:00Z"),
                Arguments.of(345, "2026-10-20T10:15:00Z"));
    }
}
