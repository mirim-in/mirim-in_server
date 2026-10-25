package com.mirim.mirimin.image.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Cloudinary 하위 폴더 */
@Getter
@RequiredArgsConstructor
public enum ImagePurpose {
    PROFILE("profile"),
    QUESTION("question"),
    NOTICE("notice");

    private final String folder;
}
