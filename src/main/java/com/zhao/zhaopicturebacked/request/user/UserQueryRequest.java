package com.zhao.zhaopicturebacked.request.user;

import com.zhao.zhaopicturebacked.request.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userAccount;
    private String userName;
    private String userIntroduction;
    private String sortField;
    private String sortOrder;

}
