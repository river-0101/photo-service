package com.nhn.cloud.photoservice.domain.album;

import com.nhn.cloud.photoservice.domain.common.BaseEntity;
import com.nhn.cloud.photoservice.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "albums", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_share_token", columnList = "share_token", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(unique = true, length = 36)
    private String shareToken;

    @Column(nullable = false)
    private Boolean isShared;

    @Builder
    public Album(User user, String title, String description) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.isShared = false;
    }

    public void updateInfo(String title, String description) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void enableSharing() {
        if (this.shareToken == null) {
            this.shareToken = UUID.randomUUID().toString();
        }
        this.isShared = true;
    }

    public void disableSharing() {
        this.isShared = false;
    }
}