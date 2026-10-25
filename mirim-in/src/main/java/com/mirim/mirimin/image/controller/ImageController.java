package com.mirim.mirimin.image.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.image.service.ImagePurpose;
import com.mirim.mirimin.image.service.ImageService;
import com.mirim.mirimin.user.service.UserReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "이미지")
@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final UserReader userReader;

    public record ImageUploadResponse(String url) {
    }

    @Operation(summary = "이미지 업로드",
            description = "질문·공지 첨부용. 받은 url을 질문 작성의 imageUrls에 넣는다. jpg/png/gif/webp, 5MB 이하")
    @PostMapping(value = "/api/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(@RequestPart("file") MultipartFile file,
                                      @RequestParam(defaultValue = "QUESTION") ImagePurpose purpose,
                                      @AuthenticationPrincipal AuthUser authUser) {
        userReader.getWritableUser(authUser);
        return new ImageUploadResponse(imageService.upload(file, purpose));
    }
}
