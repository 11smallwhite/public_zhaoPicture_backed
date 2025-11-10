package com.zhao.zhaopicturebacked.request.picture;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhao.zhaopicturebacked.request.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String searchText;
    @JsonProperty("pCategory")
    private String pCategory;
    @JsonProperty("pTags")
    private List<String> pTags;
    @JsonProperty("pSize")
    private Long pSize;
    @JsonProperty("pWidth")
    private Integer pWidth;
    @JsonProperty("pHeight")
    private Integer pHeight;
    @JsonProperty("pScale")
    private Double pScale;
    private String sortField;
    private String sortOrder;

}
