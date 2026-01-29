package com.nhn.cloud.photoservice.controller;

import com.nhn.cloud.photoservice.dto.request.PhotoUpdateRequest;
import com.nhn.cloud.photoservice.dto.request.PhotoUploadRequest;
import com.nhn.cloud.photoservice.dto.response.PhotoResponse;
import com.nhn.cloud.photoservice.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /**
     * 사진 업로드
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) PhotoUploadRequest request) {
        Long userId = Long.parseLong(authentication.getName());

        if (request == null) {
            request = new PhotoUploadRequest();
        }

        PhotoResponse response = photoService.uploadPhoto(userId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 사진 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<PhotoResponse>> getMyPhotos(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<PhotoResponse> response = photoService.getMyPhotos(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범별 사진 조회
     */
    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<PhotoResponse>> getPhotosByAlbum(
            Authentication authentication,
            @PathVariable Long albumId) {
        Long userId = Long.parseLong(authentication.getName());
        List<PhotoResponse> response = photoService.getPhotosByAlbum(userId, albumId);
        return ResponseEntity.ok(response);
    }

    /**
     * 앨범에 속하지 않은 사진 조회
     */
    @GetMapping("/no-album")
    public ResponseEntity<List<PhotoResponse>> getPhotosWithoutAlbum(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<PhotoResponse> response = photoService.getPhotosWithoutAlbum(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사진 상세 조회
     */
    @GetMapping("/{photoId}")
    public ResponseEntity<PhotoResponse> getPhoto(
            Authentication authentication,
            @PathVariable Long photoId) {
        Long userId = Long.parseLong(authentication.getName());
        PhotoResponse response = photoService.getPhoto(userId, photoId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사진 정보 수정
     */
    @PatchMapping("/{photoId}")
    public ResponseEntity<PhotoResponse> updatePhoto(
            Authentication authentication,
            @PathVariable Long photoId,
            @RequestBody PhotoUpdateRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        PhotoResponse response = photoService.updatePhoto(userId, photoId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 사진 삭제
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            Authentication authentication,
            @PathVariable Long photoId) {
        Long userId = Long.parseLong(authentication.getName());
        photoService.deletePhoto(userId, photoId);
        return ResponseEntity.noContent().build();
    }
}