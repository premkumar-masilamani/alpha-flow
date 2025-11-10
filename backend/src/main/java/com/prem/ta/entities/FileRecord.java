package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "files")
@IdClass(FileRecordId.class)
@Getter
@Setter
public class FileRecord {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Id
    @Column(name = "file_date", nullable = false)
    private OffsetDateTime fileDate;

    @Column(name = "file_download_url", nullable = false)
    private String fileDownloadUrl;

    @Column(name = "is_downloaded", nullable = false)
    private Boolean isDownloaded = false;

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @Column(name = "updated_by", length = 50)
    private String updatedBy = "admin";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
