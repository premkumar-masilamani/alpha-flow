package com.prem.ta.services;

import com.prem.ta.dtos.TradeDataDto;
import com.prem.ta.mappers.TradeDataMapper;
import com.prem.ta.repositories.TradeDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TradeDataService {

    private final TradeDataRepository tradeDataRepository;

    public TradeDataService(TradeDataRepository tradeDataRepository) {
        this.tradeDataRepository = tradeDataRepository;
    }

    public List<TradeDataDto> getTradesByTickerName(String tickerName) {

        return tradeDataRepository.findAllByTickerNameWithTicker(tickerName)
                .stream()
                .map(TradeDataMapper::toDto)
                .toList();
    }
}
