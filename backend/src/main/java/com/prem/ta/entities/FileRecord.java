package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "files")
@IdClass(FileRecordId.class)
@Data
public class FileRecord {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Id
    private OffsetDateTime fileDate;

    private String fileDownloadUrl;
    private Boolean isDownloaded = false;
    private Boolean isProcessed = false;
    private String updatedBy = "admin";
    private OffsetDateTime updatedAt = OffsetDateTime.now();

}
