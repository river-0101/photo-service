package com.nhn.cloud.photoservice.controller;

import com.nhn.cloud.photoservice.dto.request.AlbumCreateRequest;
import com.nhn.cloud.photoservice.dto.request.AlbumUpdateRequest;
import com.nhn.cloud.photoservice.dto.request.PhotoUploadRequest;
import com.nhn.cloud.photoservice.dto.response.AlbumResponse;
import com.nhn.cloud.photoservice.dto.response.PhotoResponse;
import com.nhn.cloud.photoservice.service.AlbumService;
import com.nhn.cloud.photoservice.service.PhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private final PhotoService photoService;

    /**
     * 앨범 생성
     */
    @PostMapping
    public ResponseEntity<AlbumResponse> createAlbum(
            Authentication authentication,
            @Valid @RequestBody AlbumCreateRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        AlbumResponse response = albumService.createAlbum(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 앨범 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<AlbumResponse>> getMyAlbums(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<AlbumResponse> response = albumService.getMyAlbums(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 상세 조회
     */
    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> getAlbum(
            Authentication authentication,
            @PathVariable Long albumId) {
        Long userId = Long.parseLong(authentication.getName());
        AlbumResponse response = albumService.getAlbum(userId, albumId);
        return ResponseEntity.ok(response);
    }

    /**
     * 공유 앨범 조회 (로그인 불필요)
     */
    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<AlbumResponse> getSharedAlbum(@PathVariable String shareToken) {
        AlbumResponse response = albumService.getSharedAlbum(shareToken);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 수정
     */
    @PatchMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> updateAlbum(
            Authentication authentication,
            @PathVariable Long albumId,
            @Valid @RequestBody AlbumUpdateRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        AlbumResponse response = albumService.updateAlbum(userId, albumId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범에 사진 업로드
     */
    @PostMapping("/{albumId}/photos")
    public ResponseEntity<PhotoResponse> uploadPhotoToAlbum(
            Authentication authentication,
            @PathVariable Long albumId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description) {

        Long userId = Long.parseLong(authentication.getName());

        PhotoUploadRequest request = new PhotoUploadRequest();
        request.setAlbumId(albumId);
        request.setTitle(title);
        request.setDescription(description);

        PhotoResponse response = photoService.uploadPhoto(userId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 앨범의 사진 목록 조회
     */
    @GetMapping("/{albumId}/photos")
    public ResponseEntity<List<PhotoResponse>> getAlbumPhotos(
            Authentication authentication,
            @PathVariable Long albumId) {

        Long userId = Long.parseLong(authentication.getName());
        List<PhotoResponse> response = photoService.getPhotosByAlbum(userId, albumId);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 공유 활성화
     */
    @PostMapping("/{albumId}/share")
    public ResponseEntity<AlbumResponse> enableSharing(
            Authentication authentication,
            @PathVariable Long albumId) {
        Long userId = Long.parseLong(authentication.getName());
        AlbumResponse response = albumService.enableSharing(userId, albumId);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 공유 비활성화
     */
    @DeleteMapping("/{albumId}/share")
    public ResponseEntity<AlbumResponse> disableSharing(
            Authentication authentication,
            @PathVariable Long albumId) {
        Long userId = Long.parseLong(authentication.getName());
        AlbumResponse response = albumService.disableSharing(userId, albumId);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범 삭제
     */
    @DeleteMapping("/{albumId}")
    public ResponseEntity<Void> deleteAlbum(
            Authentication authentication,
            @PathVariable Long albumId) {
        Long userId = Long.parseLong(authentication.getName());
        albumService.deleteAlbum(userId, albumId);
        return ResponseEntity.noContent().build();
    }
}