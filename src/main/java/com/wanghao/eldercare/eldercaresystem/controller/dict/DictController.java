package com.wanghao.eldercare.eldercaresystem.controller.dict;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.entity.dict.*;
import com.wanghao.eldercare.eldercaresystem.mapper.dict.*;
import java.util.Collections;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

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
