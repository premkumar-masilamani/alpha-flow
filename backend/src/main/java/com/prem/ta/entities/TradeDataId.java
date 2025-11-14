package com.prem.ta.entities;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Data
public class TradeDataId implements Serializable {

    private OffsetDateTime tradeTime;
    private Integer ticker;

}
