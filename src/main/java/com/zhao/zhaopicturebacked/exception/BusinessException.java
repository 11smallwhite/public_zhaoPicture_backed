package com.zhao.zhaopicturebacked.exception;

import com.zhao.zhaopicturebacked.enums.CodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private int code;

    public BusinessException(CodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(CodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }


}
