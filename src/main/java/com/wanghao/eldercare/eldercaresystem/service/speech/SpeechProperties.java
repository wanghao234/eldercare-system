package com.wanghao.eldercare.eldercaresystem.service.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "speech")
public class SpeechProperties {

    private boolean mockEnabled;
    private String provider = "tencent";
    private String ffmpegPath = "ffmpeg";
    private TencentProperties tencent = new TencentProperties();

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public TencentProperties getTencent() {
        return tencent;
    }

    public void setTencent(TencentProperties tencent) {
        this.tencent = tencent;
    }

    public static class TencentProperties {
        private String secretId;
        private String secretKey;
        private String region = "ap-guangzhou";

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
