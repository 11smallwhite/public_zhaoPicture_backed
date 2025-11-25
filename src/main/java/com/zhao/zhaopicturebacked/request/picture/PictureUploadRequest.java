package com.zhao.zhaopicturebacked.request.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = -1L;
    private Long id;
    private String fileUrl;
    private String picName;
    private Long spaceId;
}
