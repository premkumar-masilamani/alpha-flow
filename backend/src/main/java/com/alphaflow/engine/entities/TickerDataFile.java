package com.alphaflow.engine.entities;

import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tick_data_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerDataFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tick_data_file_id")
    private Long dataFileId;

    @Column(name = "file_date", nullable = false)
    private LocalDate dataFileDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "file_url", nullable = false)
    private String dataFileUrl;

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;
}
