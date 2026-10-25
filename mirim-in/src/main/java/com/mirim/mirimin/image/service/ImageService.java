package com.mirim.mirimin.image.service;

import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final ImageUploader imageUploader;

    /** 크기 제한(5MB)은 spring.servlet.multipart.max-file-size 에서 먼저 걸린다. */
    public String upload(MultipartFile file, ImagePurpose purpose) {
        if (file == null || file.isEmpty() || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }
        try {
            return imageUploader.upload(file.getBytes(), purpose);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}
