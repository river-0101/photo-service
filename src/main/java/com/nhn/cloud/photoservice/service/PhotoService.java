package com.nhn.cloud.photoservice.service;

import com.nhn.cloud.photoservice.domain.album.Album;
import com.nhn.cloud.photoservice.domain.photo.Photo;
import com.nhn.cloud.photoservice.domain.user.User;
import com.nhn.cloud.photoservice.dto.request.PhotoUpdateRequest;
import com.nhn.cloud.photoservice.dto.request.PhotoUploadRequest;
import com.nhn.cloud.photoservice.dto.response.PhotoResponse;
import com.nhn.cloud.photoservice.exception.CustomException;
import com.nhn.cloud.photoservice.exception.ErrorCode;
import com.nhn.cloud.photoservice.repository.AlbumRepository;
import com.nhn.cloud.photoservice.repository.PhotoRepository;
import com.nhn.cloud.photoservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final ObjectStorageService objectStorageService;

    /**
     * 사진 업로드
     */
    @Transactional
    public PhotoResponse uploadPhoto(Long userId, MultipartFile file, PhotoUploadRequest request) {
        User user = getUserById(userId);

        // 앨범 검증 (앨범이 지정된 경우)
        Album album = null;
        if (request.getAlbumId() != null) {
            album = albumRepository.findByIdAndUser(request.getAlbumId(), user)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        }

        // Object Storage에 파일 업로드
        String storageKey = objectStorageService.uploadFile(file, userId);

        // Photo 엔티티 생성 및 저장
        Photo photo = Photo.builder()
                .user(user)
                .album(album)
                .originalFilename(file.getOriginalFilename())
                .storageKey(storageKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Photo savedPhoto = photoRepository.save(photo);
        log.info("Photo uploaded: {} by user: {}", savedPhoto.getId(), userId);

        // Pre-signed URL 생성
        String downloadUrl = objectStorageService.generatePresignedUrl(storageKey);

        return PhotoResponse.from(savedPhoto, downloadUrl);
    }

    /**
     * 내 사진 목록 조회
     */
    public List<PhotoResponse> getMyPhotos(Long userId) {
        User user = getUserById(userId);

        List<Photo> photos = photoRepository.findByUserOrderByCreatedAtDesc(user);

        return photos.stream()
                .map(photo -> {
                    String downloadUrl = objectStorageService.generatePresignedUrl(photo.getStorageKey());
                    return PhotoResponse.from(photo, downloadUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 앨범별 사진 조회
     */
    public List<PhotoResponse> getPhotosByAlbum(Long userId, Long albumId) {
        User user = getUserById(userId);

        Album album = albumRepository.findByIdAndUser(albumId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        List<Photo> photos = photoRepository.findByAlbumOrderByCreatedAtDesc(album);

        return photos.stream()
                .map(photo -> {
                    String downloadUrl = objectStorageService.generatePresignedUrl(photo.getStorageKey());
                    return PhotoResponse.from(photo, downloadUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 앨범에 속하지 않은 사진 조회
     */
    public List<PhotoResponse> getPhotosWithoutAlbum(Long userId) {
        User user = getUserById(userId);

        List<Photo> photos = photoRepository.findByUserAndAlbumIsNullOrderByCreatedAtDesc(user);

        return photos.stream()
                .map(photo -> {
                    String downloadUrl = objectStorageService.generatePresignedUrl(photo.getStorageKey());
                    return PhotoResponse.from(photo, downloadUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 사진 상세 조회
     */
    public PhotoResponse getPhoto(Long userId, Long photoId) {
        User user = getUserById(userId);

        Photo photo = photoRepository.findByIdAndUser(photoId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        String downloadUrl = objectStorageService.generatePresignedUrl(photo.getStorageKey());

        return PhotoResponse.from(photo, downloadUrl);
    }

    /**
     * 사진 정보 수정
     */
    @Transactional
    public PhotoResponse updatePhoto(Long userId, Long photoId, PhotoUpdateRequest request) {
        User user = getUserById(userId);

        Photo photo = photoRepository.findByIdAndUser(photoId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        // 앨범 변경 (지정된 경우)
        if (request.getAlbumId() != null) {
            Album album = albumRepository.findByIdAndUser(request.getAlbumId(), user)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
            photo.moveToAlbum(album);
        }

        // 사진 정보 업데이트
        photo.updateInfo(request.getTitle(), request.getDescription());

        log.info("Photo updated: {} by user: {}", photoId, userId);

        String downloadUrl = objectStorageService.generatePresignedUrl(photo.getStorageKey());

        return PhotoResponse.from(photo, downloadUrl);
    }

    /**
     * 사진 삭제
     */
    @Transactional
    public void deletePhoto(Long userId, Long photoId) {
        User user = getUserById(userId);

        Photo photo = photoRepository.findByIdAndUser(photoId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        // Object Storage에서 파일 삭제
        objectStorageService.deleteFile(photo.getStorageKey());

        // DB에서 삭제
        photoRepository.delete(photo);

        log.info("Photo deleted: {} by user: {}", photoId, userId);
    }

    /**
     * 사용자 조회 헬퍼 메서드
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public List<Photo> getPhotosByAlbumId(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));

        return photoRepository.findByAlbumOrderByCreatedAtDesc(album);
    }
}