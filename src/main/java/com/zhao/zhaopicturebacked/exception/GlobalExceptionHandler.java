package com.zhao.zhaopicturebacked.exception;


import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice// 全局异常处理注解，它会捕获所有在@Controller注解的类中所有方法中抛出的异常
public class GlobalExceptionHandler{

    /**
     * @ExceptionHandler采取的是精度匹配，也就是说，如果捕获到异常类型是BusinessException，那么它会执行handleBusinessException方法，不会执行handleException方法
     * @param e
     * @return
     */

    @ExceptionHandler(value = BusinessException.class)// 指定要处理的异常类型,如果我们捕获到 BusinessException 异常，就会执行这个方法
    public BaseResponse<?> handleBusinessException(BusinessException e){
        log.error("全局异常处理捕获到了一个BusinessException异常",e);
        return ResultUtil.fail(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)// 指定要处理的异常类型,如果我们捕获到 Exception 异常，就会执行这个方法
    public BaseResponse<?> handleException(Exception e){
        log.error("全局异常处理捕获到了一个Exception异常",e);
        return ResultUtil.fail(CodeEnum.SYSTEM_ERROR.getCode(),e.getMessage());
    }

}

