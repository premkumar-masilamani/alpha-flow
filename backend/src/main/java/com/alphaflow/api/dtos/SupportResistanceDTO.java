package com.alphaflow.api.dtos;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupportResistanceDTO {
  private String timeframe;
  private String currentType;
  private Integer importance;
  private List<SRTouchPointDTO> touchPoints;
}
