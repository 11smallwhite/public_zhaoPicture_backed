package com.zhao.zhaopicturebacked.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class LoginUserVO {

    private static final long serialVersionUID = 1L;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String userAccount;
    private String userName;
    private String userIntroduction;
    private String userAvatar;
    private Integer userType;
    private Date createTime;
    private Date updateTime;
    private Date EditTime;
}
