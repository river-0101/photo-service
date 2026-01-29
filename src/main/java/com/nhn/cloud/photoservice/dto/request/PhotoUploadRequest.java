package com.nhn.cloud.photoservice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PhotoUploadRequest {
    private Long albumId;
    private String title;
    private String description;
}