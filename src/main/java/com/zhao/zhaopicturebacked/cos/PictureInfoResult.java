package com.zhao.zhaopicturebacked.cos;

import lombok.Data;

@Data
public class PictureInfoResult {
    private String format;
    private Integer width;
    private Integer height;
    private Double scale;
    private Long size;
    private String url;
    private String name;
    private String thumbnailUrl;
}
