package com.nhn.cloud.photoservice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PhotoUpdateRequest {
    private String title;
    private String description;
    private Long albumId;
}