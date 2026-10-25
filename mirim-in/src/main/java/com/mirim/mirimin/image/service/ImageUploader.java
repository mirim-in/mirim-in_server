package com.mirim.mirimin.image.service;

/** 이미지 저장소. 테스트에서는 실제 Cloudinary 대신 가짜 구현으로 바꾼다. */
public interface ImageUploader {

    /** @return 이미지의 https URL */
    String upload(byte[] bytes, ImagePurpose purpose);
}
