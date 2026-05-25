package com.wanghao.eldercare.eldercaresystem.service.speech;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechRecognitionService {

    String recognize(MultipartFile file);
}
