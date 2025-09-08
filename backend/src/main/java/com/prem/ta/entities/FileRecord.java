package com.prem.ta.entities;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "files",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_files", columnNames = {"ticker_id", "file_date", "source"})
        }
)
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", updatable = false, nullable = false)
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticker_id", nullable = false, foreignKey = @ForeignKey(name = "fk_files_ticker"))
    private Ticker ticker;

    @Column(name = "file_date", nullable = false)
    private OffsetDateTime fileDate;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "is_downloaded", nullable = false)
    private boolean downloaded = false;

    @Column(name = "is_processed", nullable = false)
    private boolean processed = false;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updatedBy = "admin";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public OffsetDateTime getFileDate() {
        return fileDate;
    }

    public void setFileDate(OffsetDateTime fileDate) {
        this.fileDate = fileDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
