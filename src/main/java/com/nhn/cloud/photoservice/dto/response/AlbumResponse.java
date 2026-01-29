package com.nhn.cloud.photoservice.dto.response;

import com.nhn.cloud.photoservice.domain.album.Album;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AlbumResponse {
    private Long id;
    private String title;
    private String description;
    private Boolean isShared;
    private String shareToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlbumResponse from(Album album) {
        return new AlbumResponse(
                album.getId(),
                album.getTitle(),
                album.getDescription(),
                album.getIsShared(),
                album.getShareToken(),
                album.getCreatedAt(),
                album.getUpdatedAt()
        );
    }
}