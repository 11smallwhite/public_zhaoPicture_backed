package com.zhao.zhaopicturebacked.request.space;

import com.zhao.zhaopicturebacked.request.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String spaceName;
    private int spaceLevel;
    private Long userId;
    private String sortField;
    private String sortOrder;

}
