package com.zhao.zhaopicturebacked.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CodeEnum {

    SUCCESS(0, "success"),
    NULL(40, "参数为空"),
    OUT_OF_RANGE(41, "参数超出范围"),
    PARAMES_ERROR(42, "参数错误"),
    SYSTEM_ERROR(50, "系统错误"),
    NOT_AUTH(43,"权限不足"),
    NOT_FOUND(44,"未找到"),;

    int code;
    String message;


}
