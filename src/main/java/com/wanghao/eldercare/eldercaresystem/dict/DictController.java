package com.wanghao.eldercare.eldercaresystem.dict;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/dicts")
public class DictController {

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;

    public DictController(DictTypeRepository dictTypeRepository, DictItemRepository dictItemRepository) {
        this.dictTypeRepository = dictTypeRepository;
        this.dictItemRepository = dictItemRepository;
    }

    @GetMapping("/types")
    public ApiResponse<List<DictType>> getTypes() {
        return ApiResponse.success(dictTypeRepository.findAllByIsActiveOrderByTypeCode(1));
    }

    @GetMapping("/items")
    public ApiResponse<List<DictItem>> getItems(@RequestParam String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return ApiResponse.success(Collections.emptyList());
        }
        return ApiResponse.success(dictItemRepository.findByTypeCodeAndIsActiveOrderBySortNoAsc(typeCode, 1));
    }
}
