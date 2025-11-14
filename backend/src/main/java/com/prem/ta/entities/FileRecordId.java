package com.prem.ta.entities;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
public class FileRecordId implements Serializable {

    private Integer ticker;
    private OffsetDateTime fileDate;

}
