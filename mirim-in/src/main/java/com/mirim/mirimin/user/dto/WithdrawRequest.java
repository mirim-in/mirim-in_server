package com.mirim.mirimin.user.dto;

/** 이메일로 가입한 계정은 비밀번호 확인이 필요하다. Google 계정은 비워 둔다. */
public record WithdrawRequest(String password) {
}
