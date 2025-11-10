package com.zhao.zhaopicturebacked.utils;

import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.exception.BusinessException;

public class ThrowUtil {

    public static void throwBusinessException(CodeEnum codeEnum,String message) {
       throw new BusinessException(codeEnum,message);
    }

}
