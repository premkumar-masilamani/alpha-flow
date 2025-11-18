package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "files", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker_id", "file_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fileId;

    // Mapping the Foreign Key relationship (M:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "file_date", nullable = false)
    private LocalDate fileDate;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "is_downloaded", nullable = false)
    private Boolean isDownloaded = false;

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;
}