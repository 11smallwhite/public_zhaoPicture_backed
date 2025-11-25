package com.zhao.zhaopicturebacked.request.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String spaceName;
    private int spaceLevel;
}
