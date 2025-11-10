package com.zhao.zhaopicturebacked;


import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.utils.ResultUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtil.success("ok");
    }

}
