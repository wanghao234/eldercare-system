package com.wanghao.eldercare.eldercaresystem.service.speech;

import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SpeechRecognitionServiceImpl implements SpeechRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(SpeechRecognitionServiceImpl.class);
    private static final String MOCK_TEXT = "今天上午九点张三老人参加康复训练，地点在康复训练室，状态良好；下午两点组织老人参加书法活动，地点在活动室，大家参与积极；晚上七点安排健康讲座，地点在多功能厅。";
    private static final String GENERIC_RECOGNITION_ERROR = "语音识别失败，请稍后重试";

    private final SpeechProperties speechProperties;

    public SpeechRecognitionServiceImpl(SpeechProperties speechProperties) {
        this.speechProperties = speechProperties;
    }

    @Override
    public String recognize(MultipartFile file) {
        if (speechProperties.isMockEnabled()) {
            return MOCK_TEXT;
        }
        if (!"tencent".equalsIgnoreCase(speechProperties.getProvider())) {
            throw badRequest("当前仅支持腾讯云语音识别配置");
        }

        SpeechProperties.TencentProperties tencent = speechProperties.getTencent();
        if (!hasUsableCredential(tencent.getSecretId()) || !hasUsableCredential(tencent.getSecretKey())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "腾讯云语音识别配置不完整", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Path tempInput = null;
        Path tempOutput = null;
        try {
            AudioPayload payload;
            String format = detectFormat(file);
            if ("webm".equals(format)) {
                tempInput = createTempFile(".webm");
                file.transferTo(tempInput);
                tempOutput = createTempFile(".wav");
                convertWebmToWav(tempInput, tempOutput);
                payload = new AudioPayload(Files.readAllBytes(tempOutput), "wav");
            } else {
                payload = new AudioPayload(file.getBytes(), format);
            }
            return recognizeByTencent(payload.bytes(), payload.format(), tencent);
        } catch (TencentCloudSDKException ex) {
            log.warn("tencent speech recognition failed: {}", ex.getMessage(), ex);
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "语音识别失败，请稍后重试（腾讯云返回：" + sanitizeMessage(ex.getMessage()) + "）",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("speech recognition failed: {}", ex.getMessage(), ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, GENERIC_RECOGNITION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            deleteIfExists(tempInput);
            deleteIfExists(tempOutput);
        }
    }

    private String recognizeByTencent(byte[] audioBytes,
                                      String voiceFormat,
                                      SpeechProperties.TencentProperties tencent) throws TencentCloudSDKException {
        Credential credential = new Credential(tencent.getSecretId(), tencent.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("asr.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        AsrClient client = new AsrClient(credential, tencent.getRegion(), clientProfile);

        SentenceRecognitionRequest request = new SentenceRecognitionRequest();
        request.setEngSerViceType("16k_zh");
        request.setSourceType(1L);
        request.setVoiceFormat(voiceFormat);
        request.setData(Base64.getEncoder().encodeToString(audioBytes));
        request.setDataLen((long) audioBytes.length);
        request.setFilterDirty(0L);
        request.setFilterModal(0L);
        request.setFilterPunc(0L);
        request.setConvertNumMode(1L);
        request.setWordInfo(0L);

        SentenceRecognitionResponse response = client.SentenceRecognition(request);
        String result = response.getResult();
        if (!StringUtils.hasText(result)) {
            throw badRequest("未识别到有效语音内容，请检查录音是否为空或音频格式是否正确。");
        }
        return result.trim();
    }

    private String detectFormat(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            }
        }

        byte[] bytes = readFileBytes(file);
        String detectedFromHeader = detectFormatFromHeader(bytes);
        if (detectedFromHeader != null) {
            return detectedFromHeader;
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
            if (normalizedContentType.contains("x-m4a")) {
                return "m4a";
            }
            if (normalizedContentType.contains("webm")) {
                return "webm";
            }
            if (normalizedContentType.contains("wav")) {
                return "wav";
            }
            if (normalizedContentType.contains("mpeg")) {
                return "mp3";
            }
            if (normalizedContentType.contains("mp4") || normalizedContentType.contains("m4a")) {
                return "m4a";
            }
            if (normalizedContentType.contains("aac")) {
                return "aac";
            }
            if (normalizedContentType.contains("amr")) {
                return "amr";
            }
            if (normalizedContentType.contains("ogg")) {
                return "ogg-opus";
            }
        }
        return "wav";
    }

    private void convertWebmToWav(Path input, Path output) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                speechProperties.getFfmpegPath(),
                "-y",
                "-hide_banner",
                "-loglevel",
                "error",
                "-i",
                input.toString(),
                "-vn",
                "-ar",
                "16000",
                "-ac",
                "1",
                "-f",
                "wav",
                output.toString()
        );
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0 || !Files.exists(output) || Files.size(output) == 0L) {
                log.warn("ffmpeg convert failed: {}", processOutput);
                throw badRequest("音频转码失败，请检查 ffmpeg 是否安装或音频文件是否损坏。");
            }
        } catch (IOException ex) {
            throw badRequest("当前系统未安装 ffmpeg，请先安装 ffmpeg 后重试。");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, GENERIC_RECOGNITION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path createTempFile(String suffix) throws IOException {
        return Files.createTempFile("speech-recognition-", suffix);
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.debug("delete temp file failed: {}", path, ex);
        }
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private boolean hasUsableCredential(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("your_");
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, GENERIC_RECOGNITION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String detectFormatFromHeader(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        if (matches(bytes, 0x1A, 0x45, 0xDF, 0xA3)) {
            return "webm";
        }
        if (matchesAscii(bytes, "RIFF") && bytes.length >= 12 && matchesAscii(bytes, 8, "WAVE")) {
            return "wav";
        }
        if (matchesAscii(bytes, "ID3")
                || matches(bytes, 0xFF, 0xFB)
                || matches(bytes, 0xFF, 0xF3)
                || matches(bytes, 0xFF, 0xF2)) {
            return "mp3";
        }
        if (matchesAscii(bytes, "OggS")) {
            return "ogg-opus";
        }
        if (matchesAscii(bytes, "#!AMR")) {
            return "amr";
        }
        if (bytes.length >= 12 && matchesAscii(bytes, 4, "ftyp")) {
            return "m4a";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xF0) == 0xF0) {
            return "aac";
        }
        return null;
    }

    private boolean matches(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAscii(byte[] bytes, String value) {
        return matchesAscii(bytes, 0, value);
    }

    private boolean matchesAscii(byte[] bytes, int offset, String value) {
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        String sanitized = message.replace('\n', ' ').replace('\r', ' ').trim();
        return sanitized.length() > 120 ? sanitized.substring(0, 120) + "..." : sanitized;
    }

    private record AudioPayload(byte[] bytes, String format) {
    }
}
