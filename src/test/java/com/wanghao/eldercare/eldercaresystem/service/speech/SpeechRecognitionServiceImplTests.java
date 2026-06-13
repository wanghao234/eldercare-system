package com.wanghao.eldercare.eldercaresystem.service.speech;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeechRecognitionServiceImplTests {

    @Test
    void detectFormat_shouldUseHeaderWhenFilenameAndContentTypeAreGeneric() {
        SpeechRecognitionServiceImpl service = new SpeechRecognitionServiceImpl(new SpeechProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blob",
                "application/octet-stream",
                new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0x00, 0x00}
        );

        String format = ReflectionTestUtils.invokeMethod(service, "detectFormat", file);

        assertThat(format).isEqualTo("webm");
    }

    @Test
    void detectFormat_shouldRecognizeM4aFromHeader() {
        SpeechRecognitionServiceImpl service = new SpeechRecognitionServiceImpl(new SpeechProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blob",
                "application/octet-stream",
                new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'M', '4', 'A', ' '}
        );

        String format = ReflectionTestUtils.invokeMethod(service, "detectFormat", file);

        assertThat(format).isEqualTo("m4a");
    }

    @Test
    void recognize_shouldRejectPlaceholderTencentCredentials() {
        SpeechProperties properties = new SpeechProperties();
        properties.setProvider("tencent");
        properties.setMockEnabled(false);
        properties.getTencent().setSecretId("your_tencent_secret_id");
        properties.getTencent().setSecretKey("your_tencent_secret_key");
        SpeechRecognitionServiceImpl service = new SpeechRecognitionServiceImpl(properties);
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", new byte[]{'R', 'I', 'F', 'F'});

        assertThatThrownBy(() -> service.recognize(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("腾讯云语音识别配置不完整");
    }

    @Test
    void hasUsableCredential_shouldRejectExamplePlaceholder() {
        SpeechRecognitionServiceImpl service = new SpeechRecognitionServiceImpl(new SpeechProperties());

        Boolean usable = ReflectionTestUtils.invokeMethod(service, "hasUsableCredential", "your_tencent_secret_id");

        assertThat(usable).isFalse();
    }
}
