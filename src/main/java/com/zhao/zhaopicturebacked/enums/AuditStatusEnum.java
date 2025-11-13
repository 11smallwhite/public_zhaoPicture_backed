package com.zhao.zhaopicturebacked.enums;

import lombok.Getter;

@Getter
public enum AuditStatusEnum {

    REVIEWING(0, "审核中"),
    REVIEW_PASS(1, "审核通过"),
    REVIEW_FAIL(2, "审核失败");


    private final int code;
    private final String message;

    AuditStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public static AuditStatusEnum getByCode(int code) {
        if (code < 0 || code > values().length) {
            return null;
        }
        for (AuditStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }



}
