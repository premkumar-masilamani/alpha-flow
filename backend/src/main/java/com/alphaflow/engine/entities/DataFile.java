package com.alphaflow.engine.entities;

import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "data_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dataFileId;

    private LocalDate dataFileDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String dataFileUrl;

    private Boolean isProcessed = false;
}
