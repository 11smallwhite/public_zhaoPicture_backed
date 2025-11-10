package com.zhao.zhaopicturebacked.request.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userName;
    private String userIntroduction;
    private String userAvatar;

}
