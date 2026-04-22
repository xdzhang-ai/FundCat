package com.winter.fund.modules.sip;

/**
 * 定投模块控制器，负责对外暴露定投计划相关 HTTP 接口。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.sip.model.SipDtos;
import com.winter.fund.modules.sip.service.SipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "SIP", description = "基金定投接口")
public class SipController {

    private final SipService sipService;

    public SipController(SipService sipService) {
        this.sipService = sipService;
    }

    /**
     * 返回sips结果。
     */
    @GetMapping("/sips")
    @Operation(summary = "获取定投计划列表", description = "返回当前用户全部定投计划。")
    public List<SipDtos.SipPlanResponse> sips(@AuthenticationPrincipal CurrentUser currentUser) {
        return sipService.getSipPlans(currentUser);
    }

    @PostMapping("/sips")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建定投计划", description = "创建新的定投计划，并写入首个执行窗口前的计划状态。")
    public SipDtos.SipPlanResponse createSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody SipDtos.CreateSipPlanRequest request
    ) {
        return sipService.createSipPlan(currentUser, request);
    }

    @GetMapping("/sips/{sipPlanId}")
    @Operation(summary = "获取定投计划详情", description = "返回指定定投计划的详情。")
    public SipDtos.SipPlanResponse sip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return sipService.getSipPlan(currentUser, sipPlanId);
    }

    @GetMapping("/sips/{sipPlanId}/records")
    @Operation(summary = "获取定投执行记录", description = "返回指定定投计划的执行记录列表。")
    public List<SipDtos.SipExecutionRecordResponse> sipRecords(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return sipService.getSipPlanRecords(currentUser, sipPlanId);
    }

    @PatchMapping("/sips/{sipPlanId}")
    @Operation(summary = "修改定投计划", description = "更新金额、周期和费率，15:00 后修改将归属到下一交易日。")
    public SipDtos.SipPlanResponse updateSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId,
        @Valid @RequestBody SipDtos.UpdateSipPlanRequest request
    ) {
        return sipService.updateSipPlan(currentUser, sipPlanId, request);
    }

    @PostMapping("/sips/{sipPlanId}/pause")
    @Operation(summary = "暂停定投", description = "将指定定投计划置为暂停状态。")
    public SipDtos.SipPlanResponse pauseSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return sipService.pauseSipPlan(currentUser, sipPlanId);
    }

    @PostMapping("/sips/{sipPlanId}/resume")
    @Operation(summary = "恢复定投", description = "恢复已暂停的定投计划，并重算下一次执行时间。")
    public SipDtos.SipPlanResponse resumeSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return sipService.resumeSipPlan(currentUser, sipPlanId);
    }

    @PostMapping("/sips/{sipPlanId}/stop")
    @Operation(summary = "停止定投", description = "将指定定投计划置为停止状态，停止后不可恢复。")
    public SipDtos.SipPlanResponse stopSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return sipService.stopSipPlan(currentUser, sipPlanId);
    }
}
