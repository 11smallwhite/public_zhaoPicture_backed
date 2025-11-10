package com.zhao.zhaopicturebacked.utils;

import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.enums.CodeEnum;

public class ResultUtil {

    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(data,"success",0);
    }

    public static <T> BaseResponse<T> success(T data,String message){
        return new BaseResponse<>(data,message,0);
    }

    public static <T> BaseResponse<T> fail(int code,String message){
        return new BaseResponse<>(null,message,code);
    }

}
