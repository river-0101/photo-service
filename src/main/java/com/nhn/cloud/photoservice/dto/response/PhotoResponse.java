package com.nhn.cloud.photoservice.dto.response;

import com.nhn.cloud.photoservice.domain.photo.Photo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PhotoResponse {
    private Long id;
    private String originalFilename;
    private String title;
    private String description;
    private Long fileSize;
    private String contentType;
    private Long albumId;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PhotoResponse from(Photo photo, String downloadUrl) {
        return new PhotoResponse(
                photo.getId(),
                photo.getOriginalFilename(),
                photo.getTitle(),
                photo.getDescription(),
                photo.getFileSize(),
                photo.getContentType(),
                photo.getAlbum() != null ? photo.getAlbum().getId() : null,
                downloadUrl,
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }
}