package com.zhao.zhaopicturebacked.request.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = -1L;

    private String searchText;
    private Integer count = 10;
    private String namePrefix;

}
