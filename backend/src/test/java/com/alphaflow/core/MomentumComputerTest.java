package com.alphaflow.core;

import com.alphaflow.infrastructure.persistence.repositories.MarketStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentumComputerTest {

    @Mock
    private MarketStateRepository marketStateRepository;

    @InjectMocks
    private MomentumComputer momentumComputer;

    @Test
    void compute_callsBulkComputation() {
        when(marketStateRepository.computeCapitalMomentumBulk()).thenReturn(5);

        momentumComputer.compute();

        verify(marketStateRepository).computeCapitalMomentumBulk();
    }
}
