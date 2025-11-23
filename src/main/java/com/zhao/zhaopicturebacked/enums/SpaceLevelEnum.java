package com.zhao.zhaopicturebacked.enums;

import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    STANDARD(0,"普通版",1024*1024*100L,100),
    PROFESSIONAL(1,"专业版",1024*1024*1000L,1000),
    FLAGSHIP(2,"旗舰版",1024*1024*10000L,10000);


    private final int code;
    private final String levelName;
    private final long maxSize;
    private final long maxCount;

    SpaceLevelEnum(int code, String levelName, long maxSize, long maxCount) {
        this.code = code;
        this.levelName = levelName;
        this.maxSize = maxSize;
        this.maxCount = maxCount;
    }
    public static SpaceLevelEnum getSpaceLevelEnumByCode(int code) {
        if (code < 0 || code > values().length) {
            return null;
        }
        for (SpaceLevelEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
