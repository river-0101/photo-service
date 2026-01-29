package com.nhn.cloud.photoservice.domain.user;

import com.nhn.cloud.photoservice.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    public User(String email, String name, String password, UserRole role) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = role != null ? role : UserRole.USER;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
}