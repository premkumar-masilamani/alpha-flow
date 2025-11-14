package com.prem.ta.entities;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

public class FileRecordId implements Serializable {

    private Integer ticker;
    private OffsetDateTime fileDate;

    public FileRecordId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileRecordId that)) return false;
        return (
                Objects.equals(ticker, that.ticker) &&
                        Objects.equals(fileDate, that.fileDate)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, fileDate);
    }
}
