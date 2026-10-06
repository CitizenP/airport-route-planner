package com.airportrouteplanner.webapp.airport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UtcOffsetFormatterTests {

    private final UtcOffsetFormatter formatter = new UtcOffsetFormatter();

    @ParameterizedTest
    @CsvSource({
        "0, UTC+0",
        "-300, UTC-5",
        "330, UTC+5:30",
        "345, UTC+5:45"
    })
    void formatsFixedUtcOffsets(int offsetMinutes, String expected) {
        assertThat(formatter.format(offsetMinutes)).isEqualTo(expected);
    }
}
