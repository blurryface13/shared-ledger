package com.spvermicelli.tripledger.shared.interfaces.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 文件存储配置。
 */
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {

    private String baseDir = "../../files";
    private String publicBaseUrl = "";
    private String avatarDir = "avatar";
    private String coverDir = "cover";
    private String billingDir = "billing";
    private long maxUploadBytes = 10L * 1024L * 1024L;
    private long avatarTargetBytes = 1L * 1024L * 1024L;
    private long coverTargetBytes = 1L * 1024L * 1024L;
    private long billingTargetBytes = 3L * 1024L * 1024L;

    public Path resolveBaseDir() {
        return Paths.get(baseDir).toAbsolutePath().normalize();
    }

    public String resolvePublicBaseUrl() {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "";
        }
        return publicBaseUrl.trim().replaceAll("/+$", "");
    }

    public Path resolveAvatarDir() {
        return resolveBaseDir().resolve(avatarDir).normalize();
    }

    public Path resolveCoverDir() {
        return resolveBaseDir().resolve(coverDir).normalize();
    }

    public Path resolveBillingDir() {
        return resolveBaseDir().resolve(billingDir).normalize();
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        if (StringUtils.hasText(baseDir)) {
            this.baseDir = baseDir.trim();
        }
    }

    public String getAvatarDir() {
        return avatarDir;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = StringUtils.hasText(publicBaseUrl) ? publicBaseUrl.trim() : "";
    }

    public void setAvatarDir(String avatarDir) {
        if (StringUtils.hasText(avatarDir)) {
            this.avatarDir = avatarDir.trim();
        }
    }

    public String getCoverDir() {
        return coverDir;
    }

    public void setCoverDir(String coverDir) {
        if (StringUtils.hasText(coverDir)) {
            this.coverDir = coverDir.trim();
        }
    }

    public String getBillingDir() {
        return billingDir;
    }

    public void setBillingDir(String billingDir) {
        if (StringUtils.hasText(billingDir)) {
            this.billingDir = billingDir.trim();
        }
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public long getAvatarTargetBytes() {
        return avatarTargetBytes;
    }

    public void setAvatarTargetBytes(long avatarTargetBytes) {
        this.avatarTargetBytes = avatarTargetBytes;
    }

    public long getCoverTargetBytes() {
        return coverTargetBytes;
    }

    public void setCoverTargetBytes(long coverTargetBytes) {
        this.coverTargetBytes = coverTargetBytes;
    }

    public long getBillingTargetBytes() {
        return billingTargetBytes;
    }

    public void setBillingTargetBytes(long billingTargetBytes) {
        this.billingTargetBytes = billingTargetBytes;
    }
}
