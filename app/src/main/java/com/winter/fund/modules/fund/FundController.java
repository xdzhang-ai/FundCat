package com.winter.fund.modules.fund;

import com.winter.fund.modules.fund.model.FundDtos;
import com.winter.fund.modules.fund.service.FundService;
import com.winter.fund.modules.auth.model.CurrentUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/funds")
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    @GetMapping
    public List<FundDtos.FundCardResponse> search(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestParam(required = false) String query
    ) {
        return fundService.search(currentUser.id(), query);
    }

    @GetMapping("/{code}")
    public FundDtos.FundDetailResponse detail(@PathVariable String code) {
        return fundService.getDetail(code);
    }
}
