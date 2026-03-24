package com.fundcat.api.ops;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
public class OpsController {

    private final OpsService opsService;

    public OpsController(OpsService opsService) {
        this.opsService = opsService;
    }

    @GetMapping("/summary")
    public OpsDtos.OpsSummaryResponse summary() {
        return opsService.getSummary();
    }

    @GetMapping("/feature-flags")
    public java.util.List<OpsDtos.FeatureFlagResponse> featureFlags() {
        return opsService.getFeatureFlags();
    }

    @PatchMapping("/feature-flags/{code}")
    public OpsDtos.FeatureFlagResponse toggle(
        @PathVariable String code,
        @Valid @RequestBody OpsDtos.ToggleFlagRequest request
    ) {
        return opsService.toggle(code, request.enabled());
    }
}
