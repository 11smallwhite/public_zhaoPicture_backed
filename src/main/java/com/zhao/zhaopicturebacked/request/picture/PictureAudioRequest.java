package com.zhao.zhaopicturebacked.request.picture;


import lombok.Data;

@Data
public class PictureAudioRequest {
    private Long pictureId;
    private Integer audioStatus;
    private String audioMsg;
}
