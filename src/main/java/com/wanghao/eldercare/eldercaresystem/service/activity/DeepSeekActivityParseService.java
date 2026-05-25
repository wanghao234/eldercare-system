package com.wanghao.eldercare.eldercaresystem.service.activity;

import com.wanghao.eldercare.eldercaresystem.dto.activity.AiActivityFormVO;
import java.util.List;

public interface DeepSeekActivityParseService {

    List<AiActivityFormVO> parseActivityForms(String originalText);
}
