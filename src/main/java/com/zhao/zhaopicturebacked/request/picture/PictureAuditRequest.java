package com.zhao.zhaopicturebacked.request.picture;


import lombok.Data;

@Data
public class PictureAuditRequest {
    private Long pictureId;
    private Integer auditStatus;
    private String auditMsg;
}
