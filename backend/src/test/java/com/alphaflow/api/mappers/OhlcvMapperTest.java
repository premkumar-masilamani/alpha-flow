package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.DailyPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OhlcvMapperTest {

    @Test
    void toDTO_mapsAllFields() {
        DailyPrice daily = DailyPrice.builder()
                .priceDate(LocalDate.of(2024, 1, 2))
                .priceOpen(new BigDecimal("10.1234"))
                .priceHigh(new BigDecimal("12.5000"))
                .priceLow(new BigDecimal("9.0000"))
                .priceClose(new BigDecimal("11.7500"))
                .volume(123_456L)
                .build();

        OhlcvDTO dto = OhlcvMapper.toDTO(daily);

        assertThat(dto.priceDate()).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(dto.priceOpen()).isEqualByComparingTo("10.1234");
        assertThat(dto.priceHigh()).isEqualByComparingTo("12.5000");
        assertThat(dto.priceLow()).isEqualByComparingTo("9.0000");
        assertThat(dto.priceClose()).isEqualByComparingTo("11.7500");
        assertThat(dto.volume()).isEqualTo(123_456L);
    }
}
