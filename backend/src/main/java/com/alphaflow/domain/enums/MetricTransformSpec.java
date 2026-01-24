package com.alphaflow.domain.enums;

import java.util.Set;

public record MetricTransformSpec(
        Set<TransformationType> transformations,
        Set<WindowPeriod> periods
) {
}