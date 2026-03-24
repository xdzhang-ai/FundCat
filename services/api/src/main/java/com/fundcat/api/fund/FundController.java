package com.fundcat.api.fund;

import java.util.List;
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
    public List<FundDtos.FundCardResponse> search(@RequestParam(required = false) String query) {
        return fundService.search(query);
    }

    @GetMapping("/{code}")
    public FundDtos.FundDetailResponse detail(@PathVariable String code) {
        return fundService.getDetail(code);
    }
}
