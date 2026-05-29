package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.TickerDTO;
import com.alphaflow.persistence.entities.Ticker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TickerMapperTest {

    @Test
    void toDTO_mapsIdSymbolAndName() {
        Ticker ticker = Ticker.builder()
                .tickerId(7L)
                .tickerSymbol("AAPL")
                .tickerName("Apple Inc.")
                .isActive(true)
                .build();

        TickerDTO dto = TickerMapper.toDTO(ticker);

        assertThat(dto.tickerId()).isEqualTo(7L);
        assertThat(dto.tickerSymbol()).isEqualTo("AAPL");
        assertThat(dto.tickerName()).isEqualTo("Apple Inc.");
    }
}
