package com.nhn.cloud.photoservice.domain.photo;

import com.nhn.cloud.photoservice.domain.album.Album;
import com.nhn.cloud.photoservice.domain.common.BaseEntity;
import com.nhn.cloud.photoservice.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "photos", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_album_id", columnList = "album_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @Column(nullable = false, length = 200)
    private String originalFilename;

    @Column(nullable = false, length = 500)
    private String storageKey;  // Object Storage에 저장된 키

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Builder
    public Photo(User user, Album album, String originalFilename,
                 String storageKey, String contentType, Long fileSize,
                 String title, String description) {
        this.user = user;
        this.album = album;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.title = title;
        this.description = description;
    }

    public void updateInfo(String title, String description) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void moveToAlbum(Album album) {
        this.album = album;
    }
}