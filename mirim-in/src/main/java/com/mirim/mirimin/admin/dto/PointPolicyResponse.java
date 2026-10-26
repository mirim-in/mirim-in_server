package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.point.entity.PointAction;

/** defaultAmount: 정책을 지웠을 때의 기본값 */
public record PointPolicyResponse(PointAction action, String label, int amount, int defaultAmount) {

    public static PointPolicyResponse of(PointAction action, int amount) {
        return new PointPolicyResponse(action, action.getLabel(), amount, action.getDefaultAmount());
    }
}
