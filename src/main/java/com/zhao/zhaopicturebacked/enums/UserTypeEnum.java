package com.zhao.zhaopicturebacked.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserTypeEnum {
    USER(0),
    ADMIN(1);

    int type;


}
