package com.mirim.mirimin.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * CLOUDINARY_* 환경변수가 비어 있어도 서버는 뜨고, 업로드할 때만 I003 오류를 낸다.
 */
@Slf4j
@Component
public class CloudinaryImageUploader implements ImageUploader {

    private final Cloudinary cloudinary;
    private final String baseFolder;

    public CloudinaryImageUploader(AppProperties appProperties) {
        AppProperties.Cloudinary config = appProperties.cloudinary();
        boolean configured = config != null && notBlank(config.cloudName())
                && notBlank(config.apiKey()) && notBlank(config.apiSecret());
        this.cloudinary = configured ? new Cloudinary(ObjectUtils.asMap(
                "cloud_name", config.cloudName(),
                "api_key", config.apiKey(),
                "api_secret", config.apiSecret(),
                "secure", true)) : null;
        this.baseFolder = config != null && notBlank(config.folder()) ? config.folder() : "mirim-in";
    }

    @Override
    public String upload(byte[] bytes, ImagePurpose purpose) {
        if (cloudinary == null) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_NOT_CONFIGURED);
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", baseFolder + "/" + purpose.getFolder(),
                    "resource_type", "image"));
            return (String) result.get("secure_url");
        } catch (IOException | RuntimeException e) {
            log.error("Cloudinary 업로드 실패", e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
