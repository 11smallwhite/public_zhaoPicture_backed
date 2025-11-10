package com.zhao.zhaopicturebacked.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
@Data
public class PictureVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    // 使用 @JsonProperty 明确指定 JSON 字段名
    @JsonProperty("pUrl")
    private String pUrl;

    @JsonProperty("pName")
    private String pName;

    @JsonProperty("pIntroduction")
    private String pIntroduction;

    @JsonProperty("pTags")
    private List<String> pTags;

    @JsonProperty("pCategory")
    private String pCategory;

    @JsonProperty("pSize")
    private Long pSize;

    @JsonProperty("pWidth")
    private Integer pWidth;

    @JsonProperty("pHeight")
    private Integer pHeight;

    @JsonProperty("pScale")
    private Double pScale;

    @JsonProperty("pFormat")
    private String pFormat;

    private Long userId;
    private UserVO userVO;
    private Date createTime;
    private Date editTime;
}