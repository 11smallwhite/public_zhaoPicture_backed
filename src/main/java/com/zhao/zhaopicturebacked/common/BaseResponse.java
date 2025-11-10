package com.zhao.zhaopicturebacked.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseResponse <T>{

    T data;
    String message;
    int code;

}
