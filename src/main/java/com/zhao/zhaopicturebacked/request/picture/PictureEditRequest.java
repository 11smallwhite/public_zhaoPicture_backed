package com.zhao.zhaopicturebacked.request.picture;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @JsonProperty("pName")
    private String pName;
    @JsonProperty("pIntroduction")
    private String pIntroduction;
    @JsonProperty("pCategory")
    private String pCategory;
    @JsonProperty("pTags")
    private List<String> pTags;

    private Long spaceId;
}
